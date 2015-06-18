package com.ilyamur.showreel.zenrus

import java.util.concurrent.Executors

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.bixbite.http.simple.HttpExecutorSimple
import com.ilyamur.showreel.zenrus.http.HttpConnectionManagerZenrus
import com.ilyamur.twitter.finatra.FinatraServerWebosket
import com.softwaremill.macwire.Macwire
import com.twitter.util.FuturePool
import org.slf4j.LoggerFactory

object Server extends FinatraServerWebosket with Macwire {

    private val _log = LoggerFactory.getLogger(getClass)

    lazy val httpConnectionManager = wire[HttpConnectionManagerZenrus]
    lazy val httpExecutorSimple = wire[HttpExecutorSimple]
    lazy val yahooFinance: YahooFinance = wire[YahooFinance]
    lazy val executionService = Executors.newCachedThreadPool()
    lazy val futurePool = FuturePool(executionService)
    lazy val eventPipes = wire[EventPipes]
    lazy val appController = wire[AppController]

    val healthChecker = wire[HealthChecker]

    register(appController)

    _log.trace("initialized")
}
