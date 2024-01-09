package de.lise.fluxflow.springboot.cache.memory

fun interface ItemProvider<in TIdentifier, out TModel> {
    fun get(element: TIdentifier): TModel
}