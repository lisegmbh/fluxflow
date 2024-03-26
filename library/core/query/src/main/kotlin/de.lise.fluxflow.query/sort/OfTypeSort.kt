package de.lise.fluxflow.query.sort

class OfTypeSort<TSource, TTarget : TSource>(
    val sort: Sort<TTarget>,
) : Sort<TSource>