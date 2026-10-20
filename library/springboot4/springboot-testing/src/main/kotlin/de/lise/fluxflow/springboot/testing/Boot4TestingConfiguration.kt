package de.lise.fluxflow.springboot.testing

import de.lise.fluxflow.springboot.InMemoryPersistenceConfiguration
import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    BasicConfiguration::class,
    InMemoryPersistenceConfiguration::class
)
open class Boot4TestingConfiguration
