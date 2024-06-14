package de.lise.fluxflow.reflection.record

data class WildcardTypeReference(
    override val name: String,
    val lowerBounds: List<TypeReference>,
    val upperBounds: List<TypeReference>
): TypeReference