package de.lise.fluxflow.engine.bootstrapping

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration

class BootstrappingService(
    private val actions: List<BootstrapAction>,
    private val clock: Clock
) {
    fun setup() {
        Logger.info("Bootstrapping FluxFlow.")
        val start = clock.instant()
        actions.forEach {
            Logger.debug(
                "Running bootstrapper: {}",
                it::class.qualifiedName
            )
            it.setup()
        }
        val end = clock.instant()
        Logger.info(
            "Bootstrapping FluxFlow completed within {} seconds.",
            Duration.between(start, end).toSeconds()
        )
    }

    companion object {
        private val Logger = LoggerFactory.getLogger(BootstrappingService::class.java)!!
    }
}