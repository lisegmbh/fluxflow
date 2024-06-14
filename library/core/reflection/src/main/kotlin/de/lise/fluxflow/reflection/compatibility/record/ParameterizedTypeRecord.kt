package de.lise.fluxflow.reflection.compatibility.record

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference

data class ParameterizedTypeRecord(
    override val name: String,
    val typeArguments: List<TypeReference>
) : TypeRecord {
    var rawType: TypeReference? = null
        internal set
}