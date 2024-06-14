package de.lise.fluxflow.reflection.compatibility.reference

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference

data class WildcardTypeReference(
    override val name: String,
    val lowerBounds: List<TypeReference>,
    val upperBounds: List<TypeReference>
): TypeReference