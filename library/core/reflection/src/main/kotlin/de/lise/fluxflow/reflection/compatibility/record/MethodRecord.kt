package de.lise.fluxflow.reflection.compatibility.record

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference

data class MethodRecord(
    val name: String,
    val returnType: TypeReference?,
    val modifiers: Int,
    val isBridge: Boolean,
    val isDefault: Boolean,
    val parameters: List<ParameterRecord>,
)