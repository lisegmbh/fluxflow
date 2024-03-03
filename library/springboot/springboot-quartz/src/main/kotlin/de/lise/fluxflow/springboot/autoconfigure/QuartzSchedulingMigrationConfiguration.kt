package de.lise.fluxflow.springboot.autoconfigure

import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(
    name = ["fluxflow.migration.quartz.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
open class QuartzSchedulingMigrationConfiguration {
    @Bean
    open fun quartz(
        dataSource: DataSource
    ): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:db/main.yml"
        return liquibase
    }
}


