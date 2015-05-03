package com.ilyamur.showreel.zenrus

import java.util.concurrent.atomic.AtomicReference

import com.twitter.concurrent.Broker
import rx.lang.scala.Subscription

class AppWebsocketServerClient(broker: Broker[String]) {

    val refSubscription = new AtomicReference[Subscription]()

    def send(message: String): Unit = {
        broker ! message
    }

    def bindSubscription(subscription: Subscription): Unit = {
        refSubscription.set(subscription)
    }

    def close(): Unit = {
        val subscriptionOrNull = refSubscription.get()
        if (subscriptionOrNull != null) {
            subscriptionOrNull.unsubscribe()
        }
    }
}
