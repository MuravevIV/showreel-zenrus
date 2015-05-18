package com.ilyamur.twitter.finatra

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finatra.{Request, ResponseBuilder}
import com.twitter.util.Future

class ControllerCollectionWebsocket {
    var controllers: Seq[ControllerWebsocket] = Seq.empty

    var notFoundHandler = { request: Request =>
        render.status(404).plain("Not Found").toFuture
    }

    var errorHandler = { request: Request =>
        request.error match {
            case Some(e: com.twitter.finatra.UnsupportedMediaType) =>
                render.status(415).plain("No handler for this media type found").toFuture
            case _ =>
                render.status(500).plain("Something went wrong!").toFuture
        }

    }

    def render: ResponseBuilder = new ResponseBuilder

    def dispatch(request: FinagleRequest): Option[Future[FinagleResponse]] = {
        var response: Option[Future[FinagleResponse]] = None

        controllers.find { ctrl =>
            ctrl.route.dispatch(request) match {
                case Some(callbackResponse) =>
                    response = Some(callbackResponse)
                    true
                case None =>
                    false
            }
        }

        response
    }

    def add(controller: ControllerWebsocket) {
        notFoundHandler = controller.notFoundHandler.getOrElse(notFoundHandler)
        errorHandler = controller.errorHandler.getOrElse(errorHandler)
        controllers = controllers ++ Seq(controller)
    }

}
