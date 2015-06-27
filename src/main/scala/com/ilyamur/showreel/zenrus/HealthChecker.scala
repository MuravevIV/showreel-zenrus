package com.ilyamur.showreel.zenrus

import java.lang.{Long => JLong}
import java.util.concurrent.TimeUnit

import com.ilyamur.bixbite.http.simple.HttpExecutorSimple
import org.slf4j.LoggerFactory
import rx.functions.{Func1, Action1}
import rx.{Observable, Observer}

class HealthChecker(httpExecutorSimple: HttpExecutorSimple) {

    val CHECK_URL = System.getProperty("app.host") + "/ping"
    private val PERIOD_SEC = 300

    private val _log = LoggerFactory.getLogger(getClass)
    
    val errorLogger: Action1[Throwable] = new Action1[Throwable] {
        override def call(e: Throwable): Unit = {
            _log.error("", e)
        }
    }

    val obsHealthCheck = Observable.timer(PERIOD_SEC, PERIOD_SEC, TimeUnit.SECONDS).map[Unit](new Func1[JLong, Unit]() {
        override def call(t1: JLong): Unit = {
            val response = httpExecutorSimple.execute(CHECK_URL)
            if (!response.isSuccess) {
                _log.warn("'{}' request responds code={}", CHECK_URL, response.code)
            }
        }
    }).doOnError(errorLogger)
            .retryWhen(new RetryWithDelay(5, TimeUnit.SECONDS))
            .share()
    
    obsHealthCheck.subscribe(new Action1[Unit] {
        override def call(unit: Unit): Unit = {
            // event sink
        }
    })
}
