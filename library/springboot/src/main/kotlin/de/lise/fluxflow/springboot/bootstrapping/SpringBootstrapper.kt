package de.lise.fluxflow.springboot.bootstrapping

import de.lise.fluxflow.engine.bootstrapping.BootstrappingService
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

class SpringBootstrapper(
    private val bootstrappingService: BootstrappingService
) {
    @EventListener
    fun handleContextRefreshed(e: ContextRefreshedEvent) {
        bootstrappingService.setup()
    }
}