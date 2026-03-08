@file:OptIn(ExperimentalApi::class)

package de.lise.fluxflow.examples.ideas

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.springboot.EnableFluxFlow
import de.lise.fluxflow.springboot.EnableFluxFlowInMemoryPersistence
import de.lise.fluxflow.springboot.rest.EnableRestApi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableRestApi
@EnableFluxFlow
@EnableFluxFlowInMemoryPersistence
@SpringBootApplication
class IdeasApplication

fun main(args: Array<String>) {
	runApplication<IdeasApplication>(*args)
}