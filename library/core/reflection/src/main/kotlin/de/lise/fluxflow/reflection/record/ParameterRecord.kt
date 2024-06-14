package de.lise.fluxflow.reflection.record

import java.lang.reflect.Parameter

data class ParameterRecord(
    val name: String,
    val type: TypeReference,
    val modifiers: Int,
    private val parameter: Parameter
)