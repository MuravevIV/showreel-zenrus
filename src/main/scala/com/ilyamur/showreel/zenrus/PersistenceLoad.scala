package com.ilyamur.showreel.zenrus

import java.util.concurrent.TimeUnit

import org.slf4j.LoggerFactory
import rx.Observable.OnSubscribe
import rx.functions.{Action1, Func1}
import rx.schedulers.{Schedulers, Timestamped}
import rx.{Observable, Subscriber}

class PersistenceLoad(db: H2Database) {

    private val _log = LoggerFactory.getLogger(getClass)

    private type M = Map[String, Double]
    private type TM = Timestamped[M]
    private type LTM = List[TM]

    private val errorLogger: Action1[Throwable] = new Action1[Throwable] {
        override def call(e: Throwable): Unit = {
            _log.error("", e)
        }
    }

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

    val obsLatestLTM: Observable[LTM] = Observable
            .create(
                new OnSubscribe[Unit] {
                    override def call(s: Subscriber[_ >: Unit]): Unit = {
                        s.onNext(())
                        s.onCompleted()
                    }
                }
            )
            .observeOn(Schedulers.io())
            .map[LTM](
                new Func1[Unit, LTM]() {
                    override def call(unit: Unit): LTM = {
                        getLatestLTM
                    }
                }
            )
            .observeOn(Schedulers.computation())
            .doOnError(errorLogger)
            .retryWhen(new RetryWithDelay(5, TimeUnit.SECONDS))
}
