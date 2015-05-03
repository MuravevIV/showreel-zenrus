package com.ilyamur.finagle.websocket

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket
import com.twitter.finagle.{HttpWebSocket, Service}
import com.twitter.util.{Time, Future}

abstract class HttpWebsocketServer[C](addr: String) {

    val listeningServer = HttpWebSocket.serve(addr, new Service[WebSocket, WebSocket] {

        def apply(reqWebSocket: WebSocket): Future[WebSocket] = {
            val broker = new Broker[String]
            val client = createClient(broker)
            val offer = broker.recv
            val respWebSocket = reqWebSocket.copy(messages = offer)
            onopen(client)
            reqWebSocket.messages.foreach { message =>
                onmessage(client, message)
            }
            reqWebSocket.onClose.ensure {
                onclose(client)
            }
            Future.value(respWebSocket)
        }
    })

    def createClient(broker: Broker[String]): C
    
    def onopen(client: C): Unit = {
        // nop
    }

    def onmessage(client: C, message: String): Unit = {
        // nop
    }

    def onclose(client: C): Unit = {
        // nop
    }

    def close(deadline: Time): Future[Unit] = {
        listeningServer.close(deadline)
    }

    def close(): Future[Unit] = {
        listeningServer.close()
    }
}
