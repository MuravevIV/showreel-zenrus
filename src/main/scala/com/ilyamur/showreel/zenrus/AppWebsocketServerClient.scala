package com.ilyamur.showreel.zenrus

import com.twitter.concurrent.Broker

class AppWebsocketServerClient(broker: Broker[String]) {

    def send(message: String): Unit = {
        broker ! message
    }
}
