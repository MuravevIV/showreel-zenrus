package com.ilyamur.showreel.zenrus

import com.ilyamur.twitter.finatra.{ControllerWebsocket, WebSocketClient}
import com.twitter.util.FuturePool
import rx.functions.Action1

class AppController(eventPipes: EventPipes, futurePool: FuturePool) extends ControllerWebsocket {

    get("/") { request =>
        render.static("index.html").toFuture
    }

    websocket("/api/ws") { ws: WebSocketClient =>
        val subscription = eventPipes.obsMessages.subscribe(new Action1[String] {
            override def call(ratesMessage: String): Unit = {
                ws.send(ratesMessage)
            }
        })
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
