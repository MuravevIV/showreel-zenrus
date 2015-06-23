package com.ilyamur.showreel.zenrus

import java.math.BigDecimal
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import org.slf4j.LoggerFactory
import rx.Observable
import rx.functions.{Action1, Func1}
import rx.schedulers.{Schedulers, Timestamped}

class PersistenceSave(eventPipes: EventPipes, h2Database: H2Database) {

    private val _log = LoggerFactory.getLogger(getClass)

    private type M = Map[String, Double]
    private type TM = Timestamped[M]

    private val errorLogger: Action1[Throwable] = new Action1[Throwable] {
        override def call(e: Throwable): Unit = {
            _log.error("", e)
        }
    }

    private val selectCurrenciesQuery =
        "SELECT name, id_currency FROM currency"

    private val insertRateQuery =
        "INSERT INTO rate (id_currency_from, id_currency_to, reg_timestamp, value) VALUES (?, ?, ?, ?)"

    private lazy val currenciesMapping: Map[String, Int] = {
        h2Database.onConnection { conn =>
            h2Database.onPreparedStatement(selectCurrenciesQuery) { stmt =>
                h2Database.listResultSet(stmt) { case rset =>
                    (rset.getString(1), rset.getInt(2))
                }
            }(conn).toMap
        }
    }

    private def getIdCurrencies(s: String): (Int, Int) = {
        val nameFrom = s.substring(0, 3)
        val nameTo = s.substring(3)
        val idCurrencyFrom = currenciesMapping(nameFrom)
        val idCurrencyTo = currenciesMapping(nameTo)
        (idCurrencyFrom, idCurrencyTo)
    }

    private def persist(tm: TM): Unit = {

        h2Database.onConnection { conn =>

            h2Database.onPreparedStatement(insertRateQuery) { stmt =>

                val timestampLong = tm.getTimestampMillis
                val m: M = tm.getValue

                m.map { case (name, value) =>
                    val (idCurrencyFrom, idCurrencyTo) = getIdCurrencies(name)
                    stmt.setInt(1, idCurrencyFrom)
                    stmt.setInt(2, idCurrencyTo)
                    stmt.setTimestamp(3, new Timestamp(timestampLong))
                    stmt.setBigDecimal(4, new BigDecimal(value))
                    stmt.executeUpdate()
                }

            }(conn)

            conn.commit()
        }
    }

    private val obsPersistInDatabase: Observable[Unit] = eventPipes.obsRatesMapShared
            .observeOn(Schedulers.io())
            .map[Unit](
                new Func1[TM, Unit]() {
                    override def call(tm: TM): Unit = {
                        persist(tm)
                    }
                }
            )
            .observeOn(Schedulers.computation())

    obsPersistInDatabase
            .doOnError(errorLogger)
            .retryWhen(new RetryWithDelay(1, TimeUnit.SECONDS))
            .publish().connect()
}
