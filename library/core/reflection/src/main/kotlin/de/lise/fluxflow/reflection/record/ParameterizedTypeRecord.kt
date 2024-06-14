package de.lise.fluxflow.reflection.record

data class ParameterizedTypeRecord(
    override val name: String,
    val typeArguments: List<TypeReference>
) : TypeRecord {
    var rawType: TypeReference? = null
        internal set
}