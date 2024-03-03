package de.lise.fluxflow.query.filter

class UnsupportedFilterException(
    val filter: Filter<*>,
    message: String
) : Exception(message) {
    constructor(filter: Filter<*>) : this(
        filter,
        "unsupported filter: $filter"
    )
}