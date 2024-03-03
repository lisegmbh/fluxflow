package de.lise.fluxflow.query.sort

class UnsupportedSortException(
    val sort: Sort<*>,
    message: String
) : Exception(message) {
    constructor(sort: Sort<*>) : this(
        sort,
        "unsupported sort: $sort"
    )
}