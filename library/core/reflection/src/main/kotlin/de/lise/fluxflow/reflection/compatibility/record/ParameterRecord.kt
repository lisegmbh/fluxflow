package de.lise.fluxflow.reflection.compatibility.record

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference
import java.lang.reflect.Parameter

data class ParameterRecord(
    val name: String,
    val type: TypeReference,
    val modifiers: Int,
    private val parameter: Parameter
)