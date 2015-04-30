package com.ilyamur.showreel.zenrus

import com.twitter.concurrent.Broker
import com.twitter.finagle.HttpWebSocket

class WebsocketClient {

    private val broker = new Broker[String]
    private val offer = broker.recv
    HttpWebSocket.open(offer, "ws://localhost:8888/") onSuccess { webSocket =>
        webSocket.messages.foreach { message =>
            //
            if (message == "ping") {
                println("client: server send ping")
                broker ! "pong"
            }
            //
        }
    }
}
