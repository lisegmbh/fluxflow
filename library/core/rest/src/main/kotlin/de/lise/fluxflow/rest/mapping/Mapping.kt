package de.lise.fluxflow.rest.mapping

fun interface Mapping<TSource, out TTarget> {
    fun map(
        source: TSource
    ): TTarget

    companion object {
        fun <TSource, TTarget> Collection<TSource>.mapWith(
            mapping: Mapping<TSource, TTarget>
        ): List<TTarget> {
            return this.map {
                mapping.map(it)
            }
        }

        fun <TSource, TTarget> TSource.mapWith(
            mapping: Mapping<TSource, TTarget>
        ): TTarget {
            return mapping.map(this)
        }
    }
}