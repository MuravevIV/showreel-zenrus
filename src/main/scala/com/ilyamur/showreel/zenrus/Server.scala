package com.ilyamur.showreel.zenrus

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.ilyamur.bixbite.http.simple.HttpExecutorSimple
import com.ilyamur.showreel.zenrus.http.HttpConnectionManagerZenrus
import com.softwaremill.macwire.Macwire
import com.twitter.finatra._

object Server extends FinatraServer with Macwire {

    lazy val httpConnectionManager = wire[HttpConnectionManagerZenrus]
    lazy val httpExecutorSimple = wire[HttpExecutorSimple]
    lazy val yahooFinance: YahooFinance = wire[YahooFinance]
    lazy val appController = wire[AppController]

    register(appController)
}
