package com.ilyamur.twitter.finatra

import com.twitter.app.App
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finatra._
import com.twitter.util.{Await, Future}

class AppServiceWebsocket(controllers: ControllerCollectionWebsocket)
        extends Service[FinagleRequest, FinagleResponse] with App with Logging {

    def render: ResponseBuilder = new ResponseBuilder

    def apply(rawRequest: FinagleRequest): Future[FinagleResponse] = {
        val adaptedRequest = RequestAdapter(rawRequest)

        def handleError(t: Throwable) = {
            log.error(t, "Internal Server Error")
            adaptedRequest.error = Some(t)
            ResponseAdapter(adaptedRequest, controllers.errorHandler(adaptedRequest))
        }

        try {
            attemptRequest(rawRequest).handle {
                case t: Throwable =>
                    Await.result(handleError(t))
            }
        } catch {
            case e: Exception =>
                handleError(e)
        }
    }

    def attemptRequest(rawRequest: FinagleRequest): Future[FinagleResponse] = {
        val adaptedRequest = RequestAdapter(rawRequest)

        controllers.dispatch(rawRequest) match {
            case Some(response) =>
                response.asInstanceOf[Future[FinagleResponse]]
            case None =>
                ResponseAdapter(adaptedRequest, controllers.notFoundHandler(adaptedRequest))
        }
    }

}
