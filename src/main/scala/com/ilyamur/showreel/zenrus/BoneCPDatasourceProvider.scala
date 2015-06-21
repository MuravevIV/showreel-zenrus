package com.ilyamur.showreel.zenrus

import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}

class BoneCPDatasourceProvider {

    private val DRIVER_CLASS = "org.h2.Driver"
    private val DATA_DIR = System.getProperty("data.dir")

    private val config: BoneCPConfig = {
        val config = new BoneCPConfig()
        config.setJdbcUrl(s"jdbc:h2:file:${DATA_DIR}h2/database")
        config.setUsername("-user")
        config.setPassword("")
        config.setDefaultAutoCommit(false)
        config.setPartitionCount(1)
        config.setMinConnectionsPerPartition(2)
        config.setMaxConnectionsPerPartition(2)
        config.setIdleConnectionTestPeriodInMinutes(5)
        config
    }

    def get(): BoneCPDataSource = {
        Class.forName(DRIVER_CLASS)
        new BoneCPDataSource(config)
    }
}
