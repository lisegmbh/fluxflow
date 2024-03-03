package de.lise.fluxflow.query.sort

data class DefaultSort<TModel>(override val direction: Direction) : Sort<TModel>