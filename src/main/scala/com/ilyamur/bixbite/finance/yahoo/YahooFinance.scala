package com.ilyamur.bixbite.finance.yahoo

import java.net.URLEncoder
import java.util.Currency

import com.google.gson.Gson
import com.ilyamur.bixbite.http.simple.HttpExecutorSimple

class YahooFinance(httpExecutorSimple: HttpExecutorSimple) {

    def getCurrencyRate(curFrom: Currency, curTo: Currency): Double = {
        val content = getContent(List((curFrom, curTo)))
        val yahooCurrencyResponse = (new Gson()).fromJson(content, classOf[Response])
        yahooCurrencyResponse.getRate
    }

    def getCurrencyRateList(curList: Seq[(Currency, Currency)]): List[Double] = {
        val content = getContent(curList)
        val yahooCurrencyResponse = (new Gson()).fromJson(content, classOf[ResponseList])
        yahooCurrencyResponse.getRateList
    }

    def getCurrencyRateMap[A](curMap: Map[A, (Currency, Currency)]): Map[A, Double] = {
        val mList = curMap.toList
        val keys = mList.map(n => n._1)
        val values = mList.map(n => n._2)
        val cList = getCurrencyRateList(values)
        keys.zip(cList).toMap
    }

    private def getContent(curList: Seq[(Currency, Currency)]): String = {

        val yqlQuery: String = {
            val sb = new StringBuilder()
            sb.append("select id, Name, Rate, Date from yahoo.finance.xchange where pair in (")
            curList.foreach { case (curFrom, curTo) =>
                sb.append("\"").append(curFrom).append(curTo).append("\"").append(",")
            }
            sb.toString().substring(0, sb.toString().length - 1) + ")"
        }

        val httpQuery: String = {
            "http://query.yahooapis.com/v1/public/yql?q=" + URLEncoder.encode(yqlQuery, "UTF-8") +
                "&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys"
        }

        val hResponse = httpExecutorSimple.execute(httpQuery)

        if (hResponse.isSuccess) {
            hResponse.optContent match {
                case Some(content) =>
                    content
                case None =>
                    throw new RuntimeException("Yahoo request failure, empty response")
            }
        } else {
            throw new RuntimeException(s"Yahoo request failure, code = ${hResponse.code}")
        }
    }
}
