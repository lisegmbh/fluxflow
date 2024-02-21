package de.lise.fluxflow.query.filter

@Deprecated(
    "This alias will be removed in an upcoming version of FluxFlow. Use InFiler<TValueType> instead.",
    ReplaceWith("InFilter<TValueType>")
)
typealias AnyOfFilter<TValueType> = InFilter<TValueType>

data class InFilter<TValueType>(
    val anyOfValues: List<TValueType>,
) : Filter<TValueType>