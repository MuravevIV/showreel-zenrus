package com.ilyamur.showreel.zenrus

import java.util.Currency

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.twitter.finatra.{ControllerWebsocket, WebSocketClient}
import com.twitter.util.FuturePool

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

    websocket("/api/ws") { ws: WebSocketClient =>
        ws.onMessage { message =>
            try {
                println(s"$ws: message: $message")
            } catch {
                case t: Throwable =>
                    println(t.getMessage)
            }
            /*
            Executors.newSingleThreadScheduledExecutor().schedule(new Runnable {
                def run(): Unit = {
                    ws.send("test")
                }
            }, 10, TimeUnit.SECONDS)
            */
        }
        ws.onClose {
            println(s"$ws: closed")
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
