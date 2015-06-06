package com.ilyamur.showreel.zenrus

import java.lang.{Long => JLong}
import java.util.Currency
import java.util.concurrent.TimeUnit

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import rx.Observable
import rx.functions.Func1
import rx.observables.ConnectableObservable

class EventPipes(yahooFinance: YahooFinance) {

    val obsRatesMap: Observable[Map[String, Double]] =
        Observable.timer(0, 5, TimeUnit.SECONDS).map(new Func1[JLong, Map[String, Double]] {
            override def call(num: JLong): Map[String, Double] = {
                yahooFinance.getCurrencyRateMap(Map(
                    "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
                    "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
                ))
            }
        })

    val obsRatesMapHot: ConnectableObservable[Map[String, Double]] = obsRatesMap.publish()
    obsRatesMapHot.connect()

    val obsRatesMessage: Observable[String] = {
        obsRatesMapHot.map[String](new Func1[Map[String, Double], String] {
            override def call(ratesMap: Map[String, Double]): String = {
                ratesMap.map { case (key, value) =>
                    s"${key}:${value}"
                }.mkString(";")
            }
        })
    }.map(new Func1[String, String] {
        override def call(body: String): String = MessageRates.encode(body)
    })
}
