package com.ilyamur.showreel.zenrus

import java.lang.{Long => JLong}
import java.util.Currency
import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import rx.Observable.OnSubscribe
import rx.functions.{Action1, Func1}
import rx.schedulers.Timestamped
import rx.{Observable, Subscriber}

import scala.collection.JavaConverters._

class EventPipes(yahooFinance: YahooFinance) {

    type M = Map[String, Double]
    type TM = Timestamped[M]
    type CTM = ConcurrentLinkedQueue[TM]


    val obsRatesMap: Observable[TM] =
        Observable.timer(0, 5, TimeUnit.SECONDS).map[M](new Func1[JLong, M] {
            override def call(num: JLong): M = {
                yahooFinance.getCurrencyRateMap(Map(
                    "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
                    "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
                ))
            }
        }).timestamp()

    val obsRatesMapShared: Observable[TM] = obsRatesMap.share()


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


    val memory = new CTM()

    obsRatesMapShared.subscribe(new Action1[TM] {
        override def call(cm: TM): Unit = {
            memory.add(cm)
        }
    })


    val obsRatesCCMLast: Observable[CTM] = Observable.just(memory)

    val obsRatesCollectedStringLast: Observable[String] =
        obsRatesCCMLast.map[String](new Func1[CTM, String] {
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
