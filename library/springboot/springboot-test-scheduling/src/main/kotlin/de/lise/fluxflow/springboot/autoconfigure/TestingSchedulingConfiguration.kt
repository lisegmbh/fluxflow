package de.lise.fluxflow.springboot.autoconfigure

import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.test.scheduling.TestSchedulingService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TestingSchedulingConfiguration {
    @Bean
    @ConditionalOnMissingBean(SchedulingService::class)
    open fun scheduling(): SchedulingService {
        Logger.warn("You are currently using the in-memory/stub scheduling implementation. Consider using Quartz.")
        return TestSchedulingService()
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(TestingSchedulingConfiguration::class.java)!!
    }
}