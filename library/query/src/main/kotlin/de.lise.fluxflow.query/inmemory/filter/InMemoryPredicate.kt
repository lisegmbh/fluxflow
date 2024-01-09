package de.lise.fluxflow.query.inmemory.filter

fun interface InMemoryPredicate<TModel> {
    fun test(element: TModel): Boolean

    fun and(other: InMemoryPredicate<TModel>): InMemoryPredicate<TModel> {
        return InMemoryPredicate { element ->
            this.test(element) && other.test(element)
        }
    }

    fun <TTarget> mapping(mapper: (element: TTarget) -> TModel): InMemoryPredicate<TTarget> {
        return InMemoryPredicate {
            this.test(mapper(it))
        }
    }

    fun <TTarget> optionalMapping(mapper: (element: TTarget) -> TModel?): InMemoryPredicate<TTarget> {
        return InMemoryPredicate<TTarget> { element ->
            val mapped = mapper(element)
            mapped?.let { test(it) } ?: false
        }
    }
}