package com.ilyamur.twitter.finatra

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket

abstract class WebSocketClient(req: WebSocket, broker: Broker[String]) {

    def send(message: String): Unit = {
        try {
            broker ! message
        } catch {
            case t: Throwable => println(t.getMessage)
        }
    }

    def onMessage(f: String => Unit): Unit = {
        try {
            req.messages.foreach { message =>
                try {
                    f(message)
                } catch {
                    case t: Throwable =>
                        println(t.getMessage)
                }
            }
        } catch {
            case t: Throwable =>
                println(t.getMessage)
        }
    }

    def onClose(f: => Unit): Unit = {
        req.onClose.ensure(f)
    }
}
