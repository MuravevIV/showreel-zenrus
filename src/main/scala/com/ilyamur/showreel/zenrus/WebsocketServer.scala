package com.ilyamur.showreel.zenrus

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket
import com.twitter.finagle.{HttpWebSocket, Service}
import com.twitter.util.Future

class WebsocketServer {

    HttpWebSocket.serve(":8888", new Service[WebSocket, WebSocket] {
        def apply(req: WebSocket): Future[WebSocket] = {
            val broker = new Broker[String]
            val offer = broker.recv
            val webSocket = req.copy(messages = offer)
            req.messages.foreach {
                broker ! _.reverse
            }
            Future.value(webSocket)
        }
    })
}
