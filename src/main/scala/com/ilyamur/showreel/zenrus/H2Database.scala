package com.ilyamur.showreel.zenrus

import java.sql._

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

class H2Database(datasourceProvider: BoneCPDatasourceProvider) {

    private val log = LoggerFactory.getLogger(getClass)

    private val datasource = datasourceProvider.get()

    private type CloseableLike = {def close()}

    private def using[C <: CloseableLike, A](c: C)(f: C => A): A = {
        try {
            f(c)
        } finally {
            try {
                c.close()
            } catch {
                case e: Throwable =>
                    log.error("Closing resource", e)
            }
        }
    }

    def onConnection[A](f: Connection => A): A = {
        using(datasource.getConnection)(f)
    }

    def onPreparedStatement[A](query: String)(f: PreparedStatement => A)(conn: Connection): A = {
        using(conn.prepareStatement(query))(f)
    }

    def reduceResultSet[R](stmt: PreparedStatement)(f: (R, ResultSet) => R)(r: R): R = {
        using(stmt.executeQuery()) { rset =>
            var memo = r
            while (rset.next()) {
                memo = f(memo, rset)
            }
            memo
        }
    }

    def listResultSet[R](stmt: PreparedStatement)(f: ResultSet => R): List[R] = {
        reduceResultSet[ArrayBuffer[R]](stmt) { case (memo, rset) =>
            memo.+=(f(rset))
            memo
        }(new ArrayBuffer[R]()).toList
    }

    def update(query: String)(conn: Connection): Int = {
        onPreparedStatement(query) { stmt =>
            stmt.executeUpdate()
        }(conn)
    }

    // =======================================================================

    private val SQL_ERROR_CODE = new {
        val TABLE_ALREADY_EXISTS = 42101
        val INDEX_ALREADY_EXISTS = 42111
        val PRIMARY_KEY_VIOLATION = 23505
    }

    private val createCurrencyTableQuery =
        """
          |CREATE TABLE currency (
          |    id_currency NUMBER(4) NOT NULL,
          |	   CONSTRAINT xpk_currency PRIMARY KEY (id_currency),
          |	   name VARCHAR(20) NOT NULL,
          |    CONSTRAINT u0_currency UNIQUE (name)
          |)
        """.stripMargin

    private val createRateTableQuery =
        """
          |CREATE TABLE rate (
          |	   id_currency_from NUMBER(4) NOT NULL,
          |    CONSTRAINT xfk0_rate FOREIGN KEY (id_currency_from) REFERENCES currency(id_currency),
          |	   id_currency_to NUMBER(4) NOT NULL,
          |    CONSTRAINT xfk1_rate FOREIGN KEY (id_currency_to) REFERENCES currency(id_currency),
          |	   reg_timestamp TIMESTAMP NOT NULL,
          |	   value DECIMAL(20, 4) NOT NULL
          |)
        """.stripMargin

    private val createRateIndexQuery = "CREATE INDEX i0_rate ON rate(reg_timestamp)"

    private val insertCurrencyRUBQuery = "INSERT INTO currency(id_currency, name) VALUES (1, 'RUB')"
    private val insertCurrencyUSDQuery = "INSERT INTO currency(id_currency, name) VALUES (2, 'USD')"
    private val insertCurrencyEURQuery = "INSERT INTO currency(id_currency, name) VALUES (3, 'EUR')"

    private def silenceSqlErrors[A](sqlErrorCodes: Int*)(defaultValue: A)(f: => A): A = {
        try {
            f
        } catch {
            case e: SQLException =>
                if (sqlErrorCodes.contains(e.getErrorCode)) {
                    defaultValue
                } else {
                    throw e
                }
            case e: Throwable =>
                throw e
        }
    }

    private def createTableIfNotExists(query: String)(conn: Connection): Unit = {
        silenceSqlErrors(SQL_ERROR_CODE.TABLE_ALREADY_EXISTS)() {
            update(query)(conn)
        }
    }

    private def createIndexIfNotExists(query: String)(conn: Connection): Unit = {
        silenceSqlErrors(SQL_ERROR_CODE.INDEX_ALREADY_EXISTS)() {
            update(query)(conn)
        }
    }

    private def insertIfNotExists(query: String)(conn: Connection): Int = {
        silenceSqlErrors(SQL_ERROR_CODE.PRIMARY_KEY_VIOLATION)(0) {
            update(query)(conn)
        }
    }

    def initialize(): Unit = {

        using (datasource.getConnection) { conn =>

            createTableIfNotExists(createCurrencyTableQuery)(conn)
            createTableIfNotExists(createRateTableQuery)(conn)
            createIndexIfNotExists(createRateIndexQuery)(conn)

            insertIfNotExists(insertCurrencyRUBQuery)(conn)
            insertIfNotExists(insertCurrencyUSDQuery)(conn)
            insertIfNotExists(insertCurrencyEURQuery)(conn)

            conn.commit()
        }
    }

    initialize()
}
