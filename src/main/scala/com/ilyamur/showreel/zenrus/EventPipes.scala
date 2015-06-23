package com.ilyamur.showreel.zenrus

import java.lang.{Boolean => JBoolean, Long => JLong}
import java.util.Currency
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import org.slf4j.LoggerFactory
import rx.Observable.OnSubscribe
import rx.functions.{Action1, Func1}
import rx.schedulers.{Schedulers, Timestamped}
import rx.{Observable, Subscriber}

class EventPipes(yahooFinance: YahooFinance, persistenceLoad: PersistenceLoad) {

    private val _log = LoggerFactory.getLogger(getClass)

    type M = Map[String, Double]
    type TM = Timestamped[M]


    val errorLogger: Action1[Throwable] = new Action1[Throwable] {
        override def call(e: Throwable): Unit = {
            _log.error("", e)
        }
    }


    val obsRatesMap: Observable[TM] =
        Observable.timer(0, 5, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .map[M](
                    new Func1[JLong, M] {
                        override def call(num: JLong): M = {
                            yahooFinance.getCurrencyRateMap(Map(
                                "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
                                "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
                            ))
                        }
                    }
                )
                .observeOn(Schedulers.computation())
                .timestamp()


    val obsRatesMapShared: Observable[TM] = obsRatesMap
            .doOnError(errorLogger)
            .retryWhen(new RetryWithDelay(5, TimeUnit.SECONDS))
            .share()


    val ratesMapToString = new Func1[TM, String] {
        override def call(cm: TM): String = {
            cm.getTimestampMillis + "!" + cm.getValue.map { case (key, value) =>
                s"${key}:${value}"
            }.mkString(";")
        }
    }


    val obsRatesStringShared: Observable[String] = {
        obsRatesMapShared.map[String](ratesMapToString)
    }.share()


    val obsRatesCollectedStringLast: Observable[String] = {

        type LTM = List[TM]

        persistenceLoad.obsLatestLTM.map[String](new Func1[LTM, String] {
            override def call(tmList: LTM): String = {
                tmList.view.map { tm =>
                    ratesMapToString.call(tm)
                }.mkString("|")
            }
        })
    }

    val obsMessageRatesShared: Observable[String] = obsRatesStringShared.map[String](new Func1[String, String] {
        override def call(body: String): String = {
            MessageRates.encode(body)
        }
    }).share()


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


    val obsMessageRateLatest: Observable[String] = {

        val _refRateLatest = new AtomicReference[String]()

        obsRatesStringShared.subscribe(new Action1[String]() {
            override def call(s: String): Unit = {
                _refRateLatest.set(s)
            }
        })

        val obsRateLatest: Observable[String] = Observable.create(new OnSubscribe[String] {
            override def call(s: Subscriber[_ >: String]): Unit = {
                s.onNext(_refRateLatest.get())
                s.onCompleted()
            }
        })

        obsRateLatest.map[String](new Func1[String, String] {
            override def call(body: String): String = {
                MessageRateLatest.encode(body)
            }
        })
    }

    val obsMessages: Observable[String] = Observable.merge(
        obsMessageServerTimestamp,
        obsMessageRateLatest,
        obsMessageRatesShared,
        obsMessageRatesCollection
    )
}
