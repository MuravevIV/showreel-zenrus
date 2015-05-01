package com.ilyamur.finagle.websocket

import com.twitter.concurrent.{Offer, Broker}
import com.twitter.finagle.HttpWebSocket
import com.twitter.finagle.websocket.WebSocket
import com.twitter.util.Future

abstract class HttpWebsocketClient(addr: String) {

    private val broker = new Broker[String]
    private val offer = broker.recv

    private val futWebSocket = HttpWebSocket.open(offer, addr)

    futWebSocket onSuccess { webSocket =>
        onopen()
        webSocket.messages.foreach { message =>
            onmessage(message)
        }
        webSocket.onClose.ensure {
            onclose()
        }
    }

    def send(message: String): Future[Unit] = {
        broker ! message
    }

    def onopen(): Unit

    def onmessage(message: String): Unit

    def onclose(): Unit

    def close(): Unit = {
        futWebSocket.onSuccess { webSocket =>
            webSocket.close()
        }
    }
}
