package com.ilyamur.finagle.websocket

import com.twitter.concurrent.Broker

class SimpleHttpWebsocketServerClient(broker: Broker[String]) {

    def send(message: String): Unit = {
        broker ! message
    }
}
