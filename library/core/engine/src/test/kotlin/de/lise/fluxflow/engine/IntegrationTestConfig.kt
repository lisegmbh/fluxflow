package de.lise.fluxflow.engine

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    basePackageClasses = [IntegrationTestConfig::class],
)
open class IntegrationTestConfig