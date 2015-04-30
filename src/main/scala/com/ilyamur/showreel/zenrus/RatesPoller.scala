package com.ilyamur.showreel.zenrus

import java.util.Currency

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import rx.lang.scala.Observable

import scala.concurrent.duration._

class RatesPoller(yahooFinance: YahooFinance) {

    def poll(rates: Map[String, String]): Observable[String] = {
        val curMap = getCurMap(rates)
        Observable
            .timer(0.seconds, 5.seconds)
            .map { _ =>
                requestYahooFinance(curMap)
            }
    }

    def getCurMap(rates: Map[String, String]): Map[String, (Currency, Currency)] = {
        rates.map { case (fromRate, toRate) =>
            fromRate + toRate ->(Currency.getInstance(fromRate), Currency.getInstance(toRate))
        }
    }

    private def requestYahooFinance(curMap: Map[String, (Currency, Currency)]): String = {
        yahooFinance
            .getCurrencyRateMap(curMap)
            .map { case (key, value) =>
                s"${key}:${value}"
            }
            .mkString(";")
    }
}
