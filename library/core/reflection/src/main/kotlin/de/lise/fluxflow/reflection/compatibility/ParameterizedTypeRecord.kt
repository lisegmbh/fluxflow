package de.lise.fluxflow.reflection.compatibility

data class ParameterizedTypeRecord(
    override val name: String,
    val typeArguments: List<TypeReference>
) : TypeRecord {
    var rawType: TypeReference? = null
        internal set
}