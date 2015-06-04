package com.ilyamur.showreel.zenrus

import java.util.Currency

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.twitter.finatra.{ControllerWebsocket, WebSocketClient}
import com.twitter.util.FuturePool
import rx.lang.scala.{Subscription, Observable}
import scala.concurrent.duration._

class AppController(yahooFinance: YahooFinance, futurePool: FuturePool) extends ControllerWebsocket {

    get("/") { request =>
        render.static("index.html").toFuture
    }

    get("/ws") { request =>
        render.static("ws.html").toFuture
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

    val obsRates: Observable[String] = Observable.timer(0 seconds, 5 seconds).map { _ =>
        println(s"Polling from ${Thread.currentThread()}")
        val ratesMessageCode = 0.asInstanceOf[Char]
        ratesMessageCode + yahooFinance.getCurrencyRateMap(Map(
            "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
            "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
        )).map { case (key, value) =>
            s"${key}:${value}"
        }.mkString(";")
    }

    val obsRatesHot = obsRates.publish
    obsRatesHot.connect

    websocket("/api/ws") { ws: WebSocketClient =>
        val subscription: Subscription = obsRatesHot.subscribe { ratesMessage =>
            ws.send(ratesMessage)
        }
        ws.onClose {
            subscription.unsubscribe()
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
