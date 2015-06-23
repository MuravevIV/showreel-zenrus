package com.ilyamur.showreel.zenrus

import java.util.concurrent.TimeUnit

import rx.Observable
import rx.functions.Func1

class RetryWithDelay(delay: Long, unit: TimeUnit) extends Func1[Observable[_ <: Throwable], Observable[_]] {

    override def call(obs: Observable[_ <: Throwable]): Observable[_] = {
        obs.flatMap[Any](new Func1[Throwable, Observable[_]] {
            override def call(t1: Throwable): Observable[_] = {
                Observable.timer(delay, unit)
            }
        })
    }
}
