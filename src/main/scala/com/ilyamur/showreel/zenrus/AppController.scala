package com.ilyamur.showreel.zenrus

import com.ilyamur.bixbite.finance.yahoo.YahooFinance
import com.twitter.finatra.Controller

class AppController(yahooFinance: YahooFinance) extends Controller {
    
    get("/") { request =>
        render.static("index.html").toFuture
    }

    error { request =>
        request.error match {
            case _ =>
                render.status(500).plain("oops, something went wrong!").toFuture
        }
    }

    notFound { request =>
        render.status(404).plain("huh?").toFuture
    }
}
