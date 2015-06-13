package com.ilyamur.twitter.finatra

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket
import org.slf4j.LoggerFactory

abstract class WebSocketClient(req: WebSocket, broker: Broker[String]) {

    private val _log = LoggerFactory.getLogger(getClass)

    def send(message: String): Unit = {
        broker ! message
    }

    def onMessage(f: String => Unit): Unit = {
        req.messages.foreach { message =>
            try {
                f(message)
            } catch {
                case e: Throwable =>
                    _log.error("", e);
            }
        }
    }

    def onClose(f: => Unit): Unit = {
        req.onClose.ensure(f)
    }
}
