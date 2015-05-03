package com.ilyamur.showreel.zenrus

import com.ilyamur.finagle.websocket.HttpWebsocketServer
import com.twitter.concurrent.Broker
import org.slf4j.LoggerFactory

class AppWebsocketServer(ratesPoller: RatesPoller)
        extends HttpWebsocketServer[AppWebsocketServerClient](":" + System.getProperty("websocket.port")) {

    private val log = LoggerFactory.getLogger(getClass)

    val rxRates = ratesPoller.poll(Map(
        "USD" -> "RUB",
        "EUR" -> "RUB"
    ))

    override def createClient(broker: Broker[String]): AppWebsocketServerClient = {
        new AppWebsocketServerClient(broker)
    }

    override def onopen(client: AppWebsocketServerClient) {
        log.trace(s"$client opened")
        val subscription = rxRates.subscribe { rates =>
            client.send(rates)
        }
        client.bindSubscription(subscription)
    }

    override def onmessage(client: AppWebsocketServerClient, message: String) {
        log.trace(s"$client received: $message")
    }

    override def onclose(client: AppWebsocketServerClient)  {
        log.trace(s"$client closed")
        client.close()
    }
}
