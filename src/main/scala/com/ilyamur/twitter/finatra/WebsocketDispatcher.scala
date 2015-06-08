package com.ilyamur.twitter.finatra

import com.twitter.concurrent.Broker
import com.twitter.finagle.websocket.WebSocket
import org.jboss.netty.channel.{ChannelEvent, ChannelHandlerContext, ChannelUpstreamHandler, UpstreamMessageEvent}
import org.slf4j.LoggerFactory

class WebsocketDispatcher(callback: WebSocketClient => Unit) extends ChannelUpstreamHandler {

    private val _log = LoggerFactory.getLogger(getClass)

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
                                _log.error("Websocket request failure", t)
                        }
                    case _ =>
                        ctx.sendUpstream(e)
                }
            case _ =>
                ctx.sendUpstream(e)
        }
    }
}
