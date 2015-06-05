package com.ilyamur.showreel.zenrus

import java.util.Currency
import java.util.concurrent.TimeUnit

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.twitter.finatra.{ControllerWebsocket, WebSocketClient}
import com.twitter.util.FuturePool
import rx.lang.scala.{Subscription, Observable, Observer}
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

    object Message {

        trait Type { val code: Int }
        case object Rates extends Type { val code = 0 }

        def encode(messageBody: String, t: Type): String = {
            t.code.asInstanceOf[Char] + messageBody
        }
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
    }.map { messageBody =>
        Message.encode(messageBody, Message.Rates)
    }

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
