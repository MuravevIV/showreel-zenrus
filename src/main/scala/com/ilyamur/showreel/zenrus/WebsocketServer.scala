package com.ilyamur.showreel.zenrus

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket
import com.twitter.finagle.{HttpWebSocket, Service}
import com.twitter.util.Future

class WebsocketServer {

    HttpWebSocket.serve(":8888", new Service[WebSocket, WebSocket] {
        def apply(reqWebSocket: WebSocket): Future[WebSocket] = {
            val broker = new Broker[String]
            val offer = broker.recv
            val respWebSocket = reqWebSocket.copy(messages = offer)
            reqWebSocket.messages.foreach { message =>
                if (message == "pong") {
                    println("server: client answered pong")
                }
            }
            //
            println("server: sending ping to client")
            broker ! "ping"
            //
            Future.value(respWebSocket)
        }
    })

    def handler(message: String): String = {
        message.reverse
    }
}
