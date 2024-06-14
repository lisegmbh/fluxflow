package de.lise.fluxflow.reflection.compatibility

data class WildcardTypeReference(
    override val name: String,
    val lowerBounds: List<TypeReference>,
    val upperBounds: List<TypeReference>
): TypeReference