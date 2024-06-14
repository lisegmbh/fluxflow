package de.lise.fluxflow.reflection.record

data class MethodRecord(
    val name: String,
    val returnType: TypeReference?,
    val modifiers: Int,
    val isBridge: Boolean,
    val isDefault: Boolean,
    val parameters: List<ParameterRecord>,
)