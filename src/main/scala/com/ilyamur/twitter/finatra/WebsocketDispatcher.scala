package com.ilyamur.twitter.finatra

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket
import org.jboss.netty.channel.{ChannelEvent, ChannelHandlerContext, ChannelUpstreamHandler, UpstreamMessageEvent}

class WebsocketDispatcher(callback: WebSocketClient => Unit) extends ChannelUpstreamHandler {

    def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
        e match {
            case event: UpstreamMessageEvent =>
                event.getMessage match {
                    case reqWebSocket: WebSocket =>
                        try {
                            val broker = new Broker[String]
                            val offer = broker.recv
                            val respWebSocket = reqWebSocket.copy(messages = offer)
                            val webSocketClient = new WebSocketClient(reqWebSocket, broker) {}
                            callback(webSocketClient)
                            ctx.getChannel.write(respWebSocket)
                        } catch {
                            case t: Throwable =>
                                println(t.getMessage)
                        }
                    case _ =>
                        ctx.sendUpstream(e)
                }
            case _ =>
                ctx.sendUpstream(e)
        }
    }
}
