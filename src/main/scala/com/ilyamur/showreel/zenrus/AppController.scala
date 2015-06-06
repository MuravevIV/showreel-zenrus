package com.ilyamur.showreel.zenrus

import java.util.Currency

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.twitter.finatra.{ControllerWebsocket, WebSocketClient}
import com.twitter.util.FuturePool
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._

class AppController(yahooFinance: YahooFinance, futurePool: FuturePool) extends ControllerWebsocket {

    get("/") { request =>
        render.static("index.html").toFuture
    }
    
    val obsRatesMap: Observable[Map[String, Double]] = Observable.timer(0 seconds, 5 seconds).map { _ =>
        yahooFinance.getCurrencyRateMap(Map(
            "USDRUB" ->(Currency.getInstance("USD"), Currency.getInstance("RUB")),
            "EURRUB" ->(Currency.getInstance("EUR"), Currency.getInstance("RUB"))
        ))
    }

    val obsRatesMapHot = obsRatesMap.publish
    obsRatesMapHot.connect

    val obsRatesMessage: Observable[String] = {
        obsRatesMapHot.map { ratesMap =>
            ratesMap.map { case (key, value) =>
                s"${key}:${value}"
            }.mkString(";")
        }
    }.map(MessageRates.encode)

    websocket("/api/ws") { ws: WebSocketClient =>
        val subscription: Subscription = obsRatesMessage.subscribe { ratesMessage =>
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
