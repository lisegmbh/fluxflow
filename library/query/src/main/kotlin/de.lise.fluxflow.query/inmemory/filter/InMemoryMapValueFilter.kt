package de.lise.fluxflow.query.inmemory.filter

class InMemoryMapValueFilter<TKey, TValue>(
    private val key: TKey,
    private val filterForValue: InMemoryFilter<TValue>
) : InMemoryFilter<Map<TKey, TValue>> {
    override fun toPredicate(): InMemoryPredicate<Map<TKey, TValue>> {
        val valuePredicate = filterForValue.toPredicate()
        return InMemoryPredicate {
            it[key]?.let { mapValue -> 
                valuePredicate.test(mapValue) 
            } ?: false    
        }
    }
}