package com.ilyamur.netty.handler

import com.ilyamur.twitter.finatra.{ControllerWebsocket, WebsocketDispatcher}
import com.twitter.finagle.websocket.WebSocketServerHandler
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpRequestDecoder, HttpResponseEncoder}

import scala.collection.JavaConverters._

class WebsocketUpgrade(controllers: Seq[ControllerWebsocket]) extends ChannelUpstreamHandler {

    def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
        e match {
            case event: UpstreamMessageEvent =>
                event.getMessage match {
                    case httpRequest: HttpRequest =>

                        val optCallback = controllers.map { controller =>
                            controller.routesWebsocket.vector.find {
                                case (pattern, callback) =>
                                    val thematch = pattern(httpRequest.getUri.split('?').head)
                                    (thematch.orNull != null)
                                case _ =>
                                    false
                            }
                        }.headOption.flatten.map(_._2)

                        optCallback match {
                            case Some(callback) =>
                                val channel = ctx.getChannel
                                val p = channel.getPipeline

                                val websocketUpgrade = p.getContext(classOf[WebsocketUpgrade])
                                val websocketUpgradeName = websocketUpgrade.getName

                                p.toMap.asScala.foreach { case (name, handler) =>
                                    if (name != websocketUpgradeName) {
                                        p.remove(name)
                                    }
                                }

                                p.addBefore(websocketUpgradeName, "httpDecoder", new HttpRequestDecoder())
                                p.addBefore(websocketUpgradeName, "httpEncoder", new HttpResponseEncoder())
                                p.addAfter(websocketUpgradeName, "websocketServerHandler", new WebSocketServerHandler())

                                p.remove(websocketUpgradeName)

                                p.addLast("websocketDispatcher", new WebsocketDispatcher(callback))

                            case None =>
                                // pass
                        }
                    case _ => // pass
                }
            case _ => // pass
        }
        ctx.sendUpstream(e)
    }
}
