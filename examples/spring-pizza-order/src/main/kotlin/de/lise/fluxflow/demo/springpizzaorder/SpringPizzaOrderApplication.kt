package de.lise.fluxflow.demo.springpizzaorder

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import de.lise.fluxflow.springboot.EnableFluxFlow
import de.lise.fluxflow.springboot.EnableFluxFlowInMemoryPersistence

@SpringBootApplication
@EnableFluxFlow
@EnableFluxFlowInMemoryPersistence
class SpringPizzaOrderApplication

fun main(args: Array<String>) {
	runApplication<SpringPizzaOrderApplication>(*args)
}
