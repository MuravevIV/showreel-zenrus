package com.ilyamur.showreel.zenrus

import com.ilyamur.finagle.websocket.HttpWebsocketServer
import com.twitter.concurrent.Broker

class AppWebsocketServer(addr: String) extends HttpWebsocketServer[AppWebsocketServerClient](addr: String) {

    override def createClient(broker: Broker[String]): AppWebsocketServerClient = {
        new AppWebsocketServerClient(broker)
    }

    override def onopen(client: AppWebsocketServerClient) {
        println("server: opened, sending ping")
        client.send("ping")
    }

    override def onmessage(client: AppWebsocketServerClient, message: String) {
        if (message == "pong") {
            println("server: received pong")
        }
    }

    override def onclose(client: AppWebsocketServerClient)  {
        println("server: closed")
    }
}
