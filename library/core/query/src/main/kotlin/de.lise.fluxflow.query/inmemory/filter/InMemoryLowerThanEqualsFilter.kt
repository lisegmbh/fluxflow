package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.LowerThanEqualsFilter
import java.time.Instant

data class InMemoryLowerThanEqualsFilter<TModel>(
    private val value: TModel
) : InMemoryFilter<TModel> {
    constructor(lowerThanEqualsFilter: LowerThanEqualsFilter<TModel>) : this(lowerThanEqualsFilter.value)

    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate {
            if (it is Number && value is Number) {
                it.toDouble() <= value.toDouble()
            } else if (it is Instant && value is Instant) {
                it <= value
            } else {
                throw UnsupportedFilterModelException(this)
            }
        }
    }
}
