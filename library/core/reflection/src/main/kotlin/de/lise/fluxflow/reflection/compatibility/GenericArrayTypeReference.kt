package de.lise.fluxflow.reflection.compatibility

data class GenericArrayTypeReference(
    override val name: String,
    val componentType: TypeReference
): TypeReference