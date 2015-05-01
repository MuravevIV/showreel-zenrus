package com.ilyamur.showreel.zenrus

import java.util.concurrent.Executors

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.bixbite.http.simple.HttpExecutorSimple
import com.ilyamur.finagle.websocket.{HttpWebsocketServer, HttpWebsocketClient}
import com.ilyamur.showreel.zenrus.http.HttpConnectionManagerZenrus
import com.softwaremill.macwire.Macwire
import com.twitter.finatra._
import com.twitter.util.FuturePool

object Server extends FinatraServer with Macwire {

    lazy val httpConnectionManager = wire[HttpConnectionManagerZenrus]
    lazy val httpExecutorSimple = wire[HttpExecutorSimple]
    lazy val yahooFinance: YahooFinance = wire[YahooFinance]
    lazy val executionService = Executors.newCachedThreadPool()
    lazy val futurePool = FuturePool(executionService)
    lazy val appController = wire[AppController]

    val websocketServer = new AppWebsocketServer(":8888")
    val websocketClient = new AppWebsocketClient("ws://localhost:8888")

    register(appController)
}
