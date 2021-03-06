package com.ilyamur.showreel.zenrus

trait Message {

    val code: Int

    def encode(messageBody: String): String = {
        code.asInstanceOf[Char] + messageBody
    }
}

case object MessageRates extends Message { val code = 0 }
case object MessageRatesCollected extends Message { val code = 1 }
case object MessageServerTimestamp extends Message { val code = 2 }
case object MessageRateLatest extends Message { val code = 3 }
