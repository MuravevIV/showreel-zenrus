package com.ilyamur.showreel.zenrus

import java.lang.{Boolean => JBoolean, Long => JLong}
import java.util.Currency
import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import org.slf4j.LoggerFactory
import rx.Observable.OnSubscribe
import rx.functions.{Action1, Func1}
import rx.schedulers.Timestamped
import rx.{Observable, Subscriber}

import scala.collection.JavaConverters._

class EventPipes(yahooFinance: YahooFinance) {

    private val _log = LoggerFactory.getLogger(getClass)

    type M = Map[String, Double]
    type TM = Timestamped[M]
    type CTM = ConcurrentLinkedQueue[TM]


    val errorLogger: Action1[Throwable] = new Action1[Throwable] {
        override def call(e: Throwable): Unit = {
            _log.error("", e)
        }
    }


    val obsRatesMap: Observable[TM] =
        Observable.timer(0, 5, TimeUnit.SECONDS).map[M](new Func1[JLong, M] {
            override def call(num: JLong): M = {
                yahooFinance.getCurrencyRateMap(Map(
                    "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
                    "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
                ))
            }
        }).timestamp()

    
    def retryWithDelay(delay: Long, unit: TimeUnit): Func1[Observable[_ <: Throwable], Observable[_]] = {
        new Func1[Observable[_ <: Throwable], Observable[_]] {
            override def call(obs: Observable[_ <: Throwable]): Observable[_] = {
                obs.flatMap[Any](new Func1[Throwable, Observable[_]] {
                    override def call(t1: Throwable): Observable[_] = {
                        Observable.timer(delay, unit)
                    }
                })
            }
        }
    }

    val obsRatesMapShared: Observable[TM] = obsRatesMap
            .doOnError(errorLogger)
            .retryWhen(retryWithDelay(5, TimeUnit.SECONDS))
            .share()


    val ratesMapToString = new Func1[TM, String] {
        override def call(cm: TM): String = {
            cm.getTimestampMillis + "!" + cm.getValue.map { case (key, value) =>
                s"${key}:${value}"
            }.mkString(";")
        }
    }


    val obsRatesString: Observable[String] = {
        obsRatesMapShared.map[String](ratesMapToString)
    }


    val ctmCache = new CTM()

    obsRatesMapShared.subscribe(new Action1[TM] {
        override def call(tm: TM): Unit = {
            ctmCache.add(tm)
        }
    })


    val obsRatesCTMLast: Observable[CTM] = Observable.just(ctmCache)

    val obsRatesCollectedStringLast: Observable[String] =
        obsRatesCTMLast.map[String](new Func1[CTM, String] {
            override def call(cmList: CTM): String = {
                cmList.asScala.view.map { cm =>
                    ratesMapToString.call(cm)
                }.mkString("|")
            }
        })


    val obsMessageRates: Observable[String] = obsRatesString.map(new Func1[String, String] {
        override def call(body: String): String = MessageRates.encode(body)
    })

    val obsMessageRatesShared: Observable[String] = obsMessageRates.share()


    val obsMessageRatesCollection: Observable[String] = obsRatesCollectedStringLast.map(new Func1[String, String] {
        override def call(body: String): String = {
            MessageRatesCollected.encode(body)
        }
    })


    val obsMessageServerTimestamp: Observable[String] =
        Observable.create(new OnSubscribe[String] {
            override def call(s: Subscriber[_ >: String]): Unit = {
                s.onNext(String.valueOf(System.currentTimeMillis()))
                s.onCompleted()
            }
        }).map(new Func1[String, String] {
            override def call(body: String): String = {
                MessageServerTimestamp.encode(body)
            }
        })

    val obsMessages: Observable[String] = Observable.merge(
        obsMessageRatesShared,
        obsMessageRatesCollection,
        obsMessageServerTimestamp
    )
}
