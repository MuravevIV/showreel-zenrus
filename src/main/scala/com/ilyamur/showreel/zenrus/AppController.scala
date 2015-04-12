package com.ilyamur.showreel.zenrus

import java.util.Currency

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.twitter.finatra.Controller
import com.twitter.util.FuturePool

class AppController(yahooFinance: YahooFinance, futurePool: FuturePool) extends Controller {

    get("/") { request =>
        render.static("index.html").toFuture
    }

    get("/api/rates") { request =>
        futurePool {
            val ratesString = yahooFinance
                .getCurrencyRateMap(Map(
                    "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
                    "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
                ))
                .map { case (key, value) =>
                    s"${key}:${value}"
                }
                .mkString(";")
            render.plain(ratesString)
        }
    }

    error { request =>
        request.error match {
            case _ =>
                render.status(500).plain("oops, something went wrong!").toFuture
        }
    }

    notFound { request =>
        render.status(404).plain("huh?").toFuture
    }
}
