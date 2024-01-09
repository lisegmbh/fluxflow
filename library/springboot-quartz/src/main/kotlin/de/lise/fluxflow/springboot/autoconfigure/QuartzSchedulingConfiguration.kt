package de.lise.fluxflow.springboot.autoconfigure

import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.springboot.scheduling.quartz.QuartzSchedulingService
import org.quartz.Scheduler
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@AutoConfigureBefore(
    name = ["de.lise.fluxflow.springboot.autoconfigure.TestingSchedulingConfiguration"]
)
open class QuartzSchedulingConfiguration {
    @Bean
    @ConditionalOnBean(Scheduler::class)
    open fun schedulingService(
        scheduler: Scheduler,
        clock: Clock
    ): SchedulingService {
        return QuartzSchedulingService(
            scheduler,
            clock
        )
    }
}