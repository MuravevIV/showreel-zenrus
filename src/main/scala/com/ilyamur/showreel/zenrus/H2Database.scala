package com.ilyamur.showreel.zenrus

import java.sql.{SQLException, Connection, PreparedStatement}

import org.slf4j.LoggerFactory

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

    private def onPreparedStatement[A](query: String)(f: PreparedStatement => A)(conn: Connection): A  = {
        using (conn.prepareStatement(query))(f)
    }

    private def update(query: String)(conn: Connection): Int = {
        onPreparedStatement(query) { stmt =>
            stmt.executeUpdate()
        }(conn)
    }

    // =======================================================================

    private val SQL_ERROR_CODE = new {
        val TABLE_ALREADY_EXISTS = 42101
        val PRIMARY_KEY_VIOLATION = 23505
    }

    private val createCurrencyTableQuery = """
                                     |CREATE TABLE currency (
                                     |    id_currency NUMBER(4) NOT NULL,
                                     |	   CONSTRAINT xpk_currency PRIMARY KEY (id_currency),
                                     |	   name VARCHAR(20) NOT NULL
                                     |)
                                   """.stripMargin

    private val createRateTableQuery = """
                                 |CREATE TABLE rate (
                                 |	   id_currency_from NUMBER(4) NOT NULL,
                                 |	   id_currency_to NUMBER(4) NOT NULL,
                                 |	   reg_timestamp NUMBER(20) NOT NULL,
                                 |	   value NUMBER(20) NOT NULL
                                 |)
                               """.stripMargin

    private val insertCurrencyRUBQuery = "INSERT INTO currency VALUES (1, 'RUB')"

    private val insertCurrencyUSDQuery = "INSERT INTO currency VALUES (2, 'USD')"

    private val insertCurrencyEURQuery = "INSERT INTO currency VALUES (3, 'EUR')"

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

    private def insertRowIfNotExists(query: String)(conn: Connection): Int = {
        silenceSqlErrors(SQL_ERROR_CODE.PRIMARY_KEY_VIOLATION)(0) {
            update(query)(conn)
        }
    }

    def initialize(): Unit = {

        using (datasource.getConnection) { conn =>

            createTableIfNotExists(createCurrencyTableQuery)(conn)
            createTableIfNotExists(createRateTableQuery)(conn)

            insertRowIfNotExists(insertCurrencyRUBQuery)(conn)
            insertRowIfNotExists(insertCurrencyUSDQuery)(conn)
            insertRowIfNotExists(insertCurrencyEURQuery)(conn)

            conn.commit()
        }
    }

    initialize()
}
