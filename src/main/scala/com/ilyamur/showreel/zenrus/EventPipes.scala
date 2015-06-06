package com.ilyamur.showreel.zenrus

import java.util.Currency
import com.ilyamur.bixbite.finance.yahoo.YahooFinance

import scala.concurrent.duration._

import rx.lang.scala.Observable

class EventPipes(yahooFinance: YahooFinance) {

    val ratesMap: Observable[Map[String, Double]] = Observable.timer(0 seconds, 5 seconds).map { _ =>
        yahooFinance.getCurrencyRateMap(Map(
            "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
            "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
        ))
    }

    val ratesMapHot = ratesMap.publish
    ratesMapHot.connect

    val ratesMessage: Observable[String] = {
        ratesMapHot.map { ratesMap =>
            ratesMap.map { case (key, value) =>
                s"${key}:${value}"
            }.mkString(";")
        }
    }.map(MessageRates.encode)
}
