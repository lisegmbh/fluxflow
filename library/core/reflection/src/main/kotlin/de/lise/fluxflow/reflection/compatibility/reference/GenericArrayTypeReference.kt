package de.lise.fluxflow.reflection.compatibility.reference

data class GenericArrayTypeReference(
    override val name: String,
    val componentType: TypeReference
): TypeReference