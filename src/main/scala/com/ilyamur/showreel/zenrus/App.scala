package com.ilyamur.showreel.zenrus

import com.twitter.finatra._

object App extends FinatraServer {

    class ExampleApp extends Controller {

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

    register(new ExampleApp())
}
