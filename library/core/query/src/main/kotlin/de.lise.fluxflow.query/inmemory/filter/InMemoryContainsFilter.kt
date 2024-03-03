package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.ContainsFilter

data class InMemoryContainsFilter(
    val content: String
) : InMemoryFilter<String> {
    constructor(containsFilter: ContainsFilter) : this(containsFilter.content)

    override fun toPredicate(): InMemoryPredicate<String> {
        return InMemoryPredicate { value ->
            value.contains(content)
        }
    }
}