package de.lise.fluxflow.examples.ideas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdeasApplication

fun main(args: Array<String>) {
	runApplication<IdeasApplication>(*args)
}