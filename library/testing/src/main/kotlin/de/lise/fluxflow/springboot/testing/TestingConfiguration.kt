package de.lise.fluxflow.springboot.testing

import de.lise.fluxflow.springboot.FluxFlowConfiguration
import de.lise.fluxflow.springboot.InMemoryPersistenceConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration

@Import(FluxFlowConfiguration::class, InMemoryPersistenceConfiguration::class)
open class TestingConfiguration