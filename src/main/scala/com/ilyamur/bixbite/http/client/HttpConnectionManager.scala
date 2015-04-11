package com.ilyamur.bixbite.http.client

import org.apache.http.client.HttpClient

trait HttpConnectionManager {

    def createHttpClient: HttpClient

    def close()
}
