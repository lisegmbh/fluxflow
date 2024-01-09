package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.StartsWithFilter

data class InMemoryStartsWithFilter(
    private val prefix: String
) : InMemoryFilter<String> {
    constructor(startsWithFilter: StartsWithFilter) : this(startsWithFilter.prefix)

    override fun toPredicate(): InMemoryPredicate<String> {
        return InMemoryPredicate {
            it.startsWith(prefix)
        }
    }
}