package de.lise.fluxflow.springboot.activation.parameter

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Configuration
open class ValueTestBeanConfiguration {

    companion object {
        val TestInstant: Instant = LocalDate.of(2023, 10, 25).atStartOfDay(ZoneId.systemDefault()).toInstant()
    }

    @Bean
    open fun valueTestClock(): Clock {
        return mock<Clock> {
            on { instant() } doReturn TestInstant
        }
    }
}