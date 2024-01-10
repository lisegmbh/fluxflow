package de.lise.fluxflow.demo.springpizzaorder

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Price @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @JsonValue
    val value: Double
)