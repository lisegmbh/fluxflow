package de.lise.fluxflow.reflection.compatibility

data class FieldRecord(
    val type: TypeReference,
    val name: String,
    val modifiers: Int,
    val isEnumConstant: Boolean,
)