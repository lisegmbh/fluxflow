package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.EndsWithFilter

data class InMemoryEndsWithFilter(
    private val suffix: String
) : InMemoryFilter<String> {
    constructor(endsWithFilter: EndsWithFilter) : this(endsWithFilter.suffix)

    override fun toPredicate(): InMemoryPredicate<String> {
        return InMemoryPredicate {
            it.endsWith(suffix)
        }
    }
}
