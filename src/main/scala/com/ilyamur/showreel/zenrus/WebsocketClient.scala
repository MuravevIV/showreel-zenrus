package com.ilyamur.showreel.zenrus

import com.twitter.concurrent.Broker
import com.twitter.finagle.HttpWebSocket

class WebsocketClient {

    private val broker = new Broker[String]
    private val offer = broker.recv
    HttpWebSocket.open(offer, "ws://localhost:8080/") onSuccess { webSocket =>
        webSocket.messages.foreach { message =>
            println(message)
        }
    }
}
