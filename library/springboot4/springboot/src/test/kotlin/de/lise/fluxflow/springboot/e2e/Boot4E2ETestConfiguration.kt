package de.lise.fluxflow.springboot.e2e

import de.lise.fluxflow.springboot.testing.Boot4TestingConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(Boot4TestingConfiguration::class)
@ComponentScan(basePackageClasses = [Boot4E2ETestConfiguration::class])
open class Boot4E2ETestConfiguration
