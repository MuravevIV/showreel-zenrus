package com.ilyamur.showreel.zenrus

import java.lang.{Long => JLong}
import java.util.Currency
import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import rx.Observable
import rx.functions.{Action1, Func1}

import scala.collection.JavaConverters._

class EventPipes(yahooFinance: YahooFinance) {

    type CM = Map[String, Double]
    type CCM = ConcurrentLinkedQueue[CM]


    val obsRatesMap: Observable[CM] =
        Observable.timer(0, 5, TimeUnit.SECONDS).map(new Func1[JLong, CM] {
            override def call(num: JLong): CM = {
                println("polling")
                yahooFinance.getCurrencyRateMap(Map(
                    "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
                    "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
                ))
            }
        })

    val obsRatesMapShared = obsRatesMap.share()


    val ratesMapToString = new Func1[CM, String] {
        override def call(ratesMap: CM): String = {
            ratesMap.map { case (key, value) =>
                s"${key}:${value}"
            }.mkString(";")
        }
    }


    val obsRatesString: Observable[String] = {
        obsRatesMapShared.map[String](ratesMapToString)
    }

    
    val memory = new CCM()

    obsRatesMapShared.subscribe(new Action1[CM] {
        override def call(cm: CM): Unit = {
            memory.add(cm)
        }
    })

    
    val obsRatesCCMLast: Observable[CCM] = Observable.just(memory)

    val obsRatesCollectedStringLast: Observable[String] =
        obsRatesCCMLast.map[String](new Func1[CCM, String] {
            override def call(cmList: CCM): String = {
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


    val obsMessages: Observable[String] = Observable.merge(
        obsMessageRatesShared,
        obsMessageRatesCollection
    )
}
