package de.lise.fluxflow.springboot.cache.memory

import java.lang.ref.SoftReference

open class MemoryCache<TIdentifier, TModel> {
    private val cache = HashMap<TIdentifier, SoftReference<TModel>>()

    private fun cleanup() {
        cache.entries
            .filter { it.value.get() == null }
            .forEach { cache.remove(it.key) }
    }

    fun get(identifier: TIdentifier, provider: ItemProvider<TIdentifier, TModel>): TModel {
        synchronized(cache) {
            cleanup()
            val cachedItem = cache[identifier]?.get()
            if (cachedItem != null) {
                return cachedItem
            }
            val item = provider.get(identifier)
            cache[identifier] = SoftReference(item)
            return item
        }
    }

    fun get(identifier: TIdentifier, element: TModel): TModel {
        return get(identifier) {
            element
        }
    }

    fun discard(identifier: TIdentifier) {
        synchronized(cache) {
            cache.remove(identifier)
        }
    }
}