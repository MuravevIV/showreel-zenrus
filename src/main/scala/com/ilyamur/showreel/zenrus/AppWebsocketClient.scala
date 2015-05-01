package com.ilyamur.showreel.zenrus

import com.ilyamur.finagle.websocket.HttpWebsocketClient

class AppWebsocketClient(addr: String) extends HttpWebsocketClient(addr: String) {

    override def onopen() = {
        println("client: opened")
    }

    override def onmessage(message: String) {
        if (message == "ping") {
            println("client: received ping, sending pong")
            send("pong")
        }
    }

    override def onclose() {
        println("client: closed")
    }
}
