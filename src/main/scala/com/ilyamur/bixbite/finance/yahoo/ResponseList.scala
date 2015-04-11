package com.ilyamur.bixbite.finance.yahoo

import scala.collection.JavaConverters._

class ResponseList {

    val query = new Query()

    class Query {

        val results = new Results()

        class Results {

            val rate = new java.util.ArrayList[Rate]()

            class Rate {

                val id: String = null
                val Date: String = null
                val Name: String = null
                val Rate: String = null
            }
        }
    }

    def getRate: Double = {
        val rateList = getRateList
        if (rateList.size == 0) {
            throw new RuntimeException(s"Can not define currency exchange rate: empty response list")
        } else if (rateList.size > 1) {
            throw new RuntimeException(s"Can not define currency exchange rate: mutliple response list")
        } else { // rateList.size == 1
            rateList.head
        }
    }

    def getRateList: List[Double] = {
        query.results.rate.asScala.map { r =>
            if (r.Date != "N/A") {
                r.Rate.toDouble
            } else {
                throw new RuntimeException(s"Can not define currency exchange rate: ${r.Name}")
            }
        }.toList
    }
}
