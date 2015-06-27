package com.ilyamur.showreel.zenrus

import org.slf4j.Logger
import rx.functions.Action1

class ErrorLoggingAction1(log: Logger) extends Action1[Throwable] {

    override def call(e: Throwable): Unit = {
        log.error("", e)
    }
}
