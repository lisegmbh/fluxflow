package de.lise.fluxflow.reflection.record

data class GenericArrayTypeReference(
    override val name: String,
    val componentType: TypeReference
): TypeReference