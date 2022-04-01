package com.twitter.finagle.websocket

import com.twitter.concurrent.Offer
import com.twitter.util.{Future, Promise}
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion

import java.net.URI

case class WebSocket(
  messages: Offer[String],
  uri: URI,
  headers: Map[String, String] = Map.empty[String, String],
  version: WebSocketVersion = WebSocketVersion.V13,
  onClose: Future[Unit] = new Promise[Unit],
  close: () => Unit = { () => () })
