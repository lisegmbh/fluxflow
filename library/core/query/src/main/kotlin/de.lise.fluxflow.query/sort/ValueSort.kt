package de.lise.fluxflow.query.sort

data class ValueSort<TModel>(override val direction: Direction) : Sort<TModel>