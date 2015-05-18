package com.ilyamur.twitter.finatra

import com.twitter.finatra.{PathPattern, RouteVector, SinatraPathPatternParser}

trait ControllerWebsocketSupport {

    val routesWebsocket = new RouteVector[(PathPattern, WebSocketClient => Unit)]

    def websocket(path: String)(callback: WebSocketClient => Unit) {
        val regex = SinatraPathPatternParser(path)
        routesWebsocket.add((regex, (webSocketClient) => {
            callback(webSocketClient)
        }))
    }
}
