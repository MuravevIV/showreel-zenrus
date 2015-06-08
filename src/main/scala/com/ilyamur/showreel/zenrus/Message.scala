package com.ilyamur.showreel.zenrus

trait Message {

    val code: Int

    def encode(messageBody: String): String = {
        code.asInstanceOf[Char] + messageBody
    }
}

case object MessageRates extends Message { val code = 0 }
case object MessageRatesCollected extends Message { val code = 1 }
