package de.lise.fluxflow.reflection.compatibility.reference

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference

data class TypeVariableReference(
    override val name: String,
    val bounds: List<TypeReference>
): TypeReference