package de.lise.fluxflow.reflection.compatibility.record

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference

data class FieldRecord(
    val type: TypeReference,
    val name: String,
    val modifiers: Int,
    val isEnumConstant: Boolean,
)