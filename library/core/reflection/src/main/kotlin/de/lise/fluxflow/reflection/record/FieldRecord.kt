package de.lise.fluxflow.reflection.record

data class FieldRecord(
    val type: TypeReference,
    val name: String,
    val modifiers: Int,
    val isEnumConstant: Boolean,
)