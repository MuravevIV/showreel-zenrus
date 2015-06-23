package com.ilyamur.showreel.zenrus

import org.slf4j.LoggerFactory
import rx.Observable.OnSubscribe
import rx.schedulers.Timestamped
import rx.{Observable, Subscriber}

class PersistenceLoad(db: H2Database) {

    private val _log = LoggerFactory.getLogger(getClass)

    private type M = Map[String, Double]
    private type TM = Timestamped[M]
    private type LTM = List[TM]

    val latestLTMQuery =
        """
          |SELECT
          |    r.reg_timestamp,
          |    ca.name || cb.name,
          |    r.value
          |FROM
          |    rate r
          |    JOIN currency ca ON
          |        r.id_currency_from = ca.id_currency
          |    JOIN currency cb ON
          |        r.id_currency_to = cb.id_currency
          |WHERE
          |    ca.name IN ('USD', 'EUR')
          |    AND cb.name = 'RUB'
          |    AND DATEADD(day, -1, CURRENT_TIMESTAMP) <= r.reg_timestamp
          |ORDER BY
          |    r.reg_timestamp,
          |    ca.id_currency
        """.stripMargin

    def getLatestLTM: LTM = {
        db.onConnection { conn =>
            db.onPreparedStatement(latestLTMQuery) { stmt =>
                db.listResultSet(stmt) { rset =>
                    (rset.getTimestamp(1), rset.getString(2), rset.getBigDecimal(3))
                }
            }(conn)
        }.groupBy { case (sqlTimestamp, _, _) =>
            sqlTimestamp
        }.map { case (sqlTimestamp, list) =>
            val m = list.map { case (_, rateName, rateValue) => (rateName, rateValue.doubleValue())}.toMap
            new Timestamped[M](sqlTimestamp.getTime, m)
        }.toList.sortBy { timestamped =>
            timestamped.getTimestampMillis
        }
    }

    val obsLatestLTM: Observable[LTM] = Observable.create(new OnSubscribe[LTM] {

        override def call(s: Subscriber[_ >: LTM]): Unit = {
            s.onNext(getLatestLTM)
            s.onCompleted()
        }
    })
}
