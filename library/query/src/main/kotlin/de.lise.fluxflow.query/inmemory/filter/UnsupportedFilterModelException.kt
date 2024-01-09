package de.lise.fluxflow.query.inmemory.filter

class UnsupportedFilterModelException(
    val filter: InMemoryFilter<*>,
    message: String
) : Exception() {
    constructor(filter: InMemoryFilter<*>) : this(
        filter,
        "unsupported filter model: $filter"
    )
}
