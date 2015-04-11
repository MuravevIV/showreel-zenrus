package com.ilyamur.bixbite.finance.yahoo

class Response {

    val query = new Query()

    class Query {

        val results = new Results()

        class Results {

            val rate = new Rate()

            class Rate {

                val id: String = null
                val Date: String = null
                val Name: String = null
                val Rate: String = null
            }
        }
    }

    def getRate: Double = {
        query.results.rate.Rate.toDouble
    }
}
