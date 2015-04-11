package com.ilyamur.bixbite.http.simple

import org.apache.http.client.ResponseHandler
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils

object ResponseHandlerSimple extends ResponseHandler[HttpResponseSimple] {

    def handleResponse(hResponse: HttpResponse): HttpResponseSimple = {
        val code = hResponse.getStatusLine.getStatusCode
        val optContent = hResponse.getEntity match {
            case null => None
            case entity => Some(EntityUtils.toString(entity))
        }
        HttpResponseSimple(code, optContent)
    }
}
