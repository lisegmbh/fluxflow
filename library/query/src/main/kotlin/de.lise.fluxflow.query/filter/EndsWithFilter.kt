package de.lise.fluxflow.query.filter

data class EndsWithFilter(
    val suffix: String,
    val ignoreCasing: Boolean
) : ValueFilter<String>