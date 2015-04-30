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
                onReceive(broker, message)
            }
            onOpen(broker)
            reqWebSocket.onClose.ensure {
                onClose(broker)
            }
            Future.value(respWebSocket)
        }
    })

    def onOpen(broker: Broker[String]) {
        println("server: sending ping to client")
        broker ! "ping"
    }

    def onReceive(broker: Broker[String], message: String) {
        if (message == "pong") {
            println("server: client answered pong")
        }
    }

    def onClose(broker: Broker[String])  {
        //
    }
}
