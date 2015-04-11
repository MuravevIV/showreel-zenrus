package com.ilyamur.bixbite.http.simple

import org.apache.http.client.methods.HttpGet
import com.ilyamur.bixbite.http.client.HttpConnectionManager
import org.apache.http.client.ResponseHandler
import org.apache.http.HttpResponse

class HttpExecutorSimple(httpConnectionManager: HttpConnectionManager) {

    def execute(httpQuery: String): HttpResponseSimple = {
        val httpClient = httpConnectionManager.createHttpClient
        httpClient.execute(new HttpGet(httpQuery), ResponseHandlerSimple)
    }
}
