package de.lise.fluxflow.mongo.e2e

import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(BasicConfiguration::class, MongoConfiguration::class)
@ComponentScan(basePackageClasses = [Boot4MongoE2ETestConfiguration::class])
open class Boot4MongoE2ETestConfiguration
