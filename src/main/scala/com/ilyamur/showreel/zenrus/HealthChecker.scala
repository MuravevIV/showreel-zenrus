package com.ilyamur.showreel.zenrus

import java.lang.{Long => JLong}
import java.util.concurrent.TimeUnit

import com.ilyamur.bixbite.http.simple.HttpExecutorSimple
import org.slf4j.LoggerFactory
import rx.Observable
import rx.functions.{Action1, Func1}
import rx.schedulers.Schedulers

class HealthChecker(httpExecutorSimple: HttpExecutorSimple) {

    val CHECK_URL = System.getProperty("app.host") + "/ping"
    private val PERIOD_SEC = 300

    private val _log = LoggerFactory.getLogger(getClass)

    private val obsHealthCheckOnce = Observable.just(())
            .observeOn(Schedulers.io())
            .map[Unit](
                new Func1[Unit, Unit]() {
                    override def call(unit: Unit): Unit = {
                        val response = httpExecutorSimple.execute(CHECK_URL)
                        if (!response.isSuccess) {
                            _log.warn("'{}' request responds code={}", CHECK_URL, response.code)
                        }
                    }
                }
            )
            .observeOn(Schedulers.computation())

    private val obsHealthCheckPeriodically = Observable.timer(PERIOD_SEC, PERIOD_SEC, TimeUnit.SECONDS)
            .flatMap(
                new Func1[JLong, Observable[Unit]]() {
                    override def call(t1: JLong): Observable[Unit] = {
                        obsHealthCheckOnce
                    }
                }
            )

    private val obsHealthCheckPeriodicallySafe =
        obsHealthCheckPeriodically
                .doOnError(new ErrorLoggingAction1(_log))
                .retryWhen(new RetryWithDelay(5, TimeUnit.SECONDS))

    obsHealthCheckPeriodicallySafe.subscribe(new Action1[Unit] {
        override def call(unit: Unit): Unit = {
            // event sink
        }
    })
}
