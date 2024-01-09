package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.GreaterThanEqualsFilter
import java.time.Instant

class InMemoryGreaterThanEqualsFilter<TModel>(
    private val value: TModel
) : InMemoryFilter<TModel> {
    constructor(greaterThanEqualsFilter: GreaterThanEqualsFilter<TModel>) : this(greaterThanEqualsFilter.value)


    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate {
            if (it is Number && value is Number) {
                it.toDouble() >= value.toDouble()
            } else if (it is Instant && value is Instant) {
                it >= value
            } else {
                throw UnsupportedFilterModelException(this)
            }
        }
    }
}
