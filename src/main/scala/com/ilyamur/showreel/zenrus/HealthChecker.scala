package com.ilyamur.showreel.zenrus

import java.lang.{Long => JLong}
import java.util.concurrent.TimeUnit

import com.ilyamur.bixbite.http.simple.HttpExecutorSimple
import org.slf4j.LoggerFactory
import rx.{Observable, Observer}

class HealthChecker(httpExecutorSimple: HttpExecutorSimple) {

    val CHECK_URL = System.getProperty("app.host") + "/ping"
    private val PERIOD_SEC = 300

    private val _log = LoggerFactory.getLogger(getClass)

    Observable.timer(PERIOD_SEC, PERIOD_SEC, TimeUnit.SECONDS).doOnEach(new Observer[JLong] {

        override def onNext(t: JLong): Unit = {
            val response = httpExecutorSimple.execute(CHECK_URL)
            if (!response.isSuccess) {
                _log.warn("'{}' request responds code={}", CHECK_URL, response.code)
            }
        }

        override def onCompleted(): Unit = {
            //
        }

        override def onError(e: Throwable): Unit = {
            _log.error("", e)
        }

    }).retry().publish().connect()
}
