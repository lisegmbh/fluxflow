package de.lise.fluxflow.springboot.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock

@SpringBootTest(
    classes = [
        QuartzBoot4ContextIT.QuartzTestConfiguration::class,
    ],
)
class QuartzBoot4ContextIT {

    @Autowired
    lateinit var scheduler: Scheduler

    @Test
    fun `should wire quartz scheduler and scheduling service on Spring Boot 4`() {
        assertThat(scheduler).isNotNull
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(QuartzSchedulingConfiguration::class)
    open class QuartzTestConfiguration {
        @Bean
        open fun clock(): Clock = Clock.systemUTC()
    }
}
