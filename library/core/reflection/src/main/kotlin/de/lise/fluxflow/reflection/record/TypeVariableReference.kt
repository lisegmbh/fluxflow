package de.lise.fluxflow.reflection.record

data class TypeVariableReference(
    override val name: String,
    val bounds: List<TypeReference>
): TypeReference