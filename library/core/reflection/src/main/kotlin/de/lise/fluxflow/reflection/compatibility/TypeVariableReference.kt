package de.lise.fluxflow.reflection.compatibility

data class TypeVariableReference(
    override val name: String,
    val bounds: List<TypeReference>
): TypeReference