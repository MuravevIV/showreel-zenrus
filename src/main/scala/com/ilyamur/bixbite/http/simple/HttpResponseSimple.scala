package com.ilyamur.bixbite.http.simple

import java.lang.String

case class HttpResponseSimple(code: Int, optContent: Option[String]) {

    def isSuccess: Boolean = {
        (code == 200)
    }
}
