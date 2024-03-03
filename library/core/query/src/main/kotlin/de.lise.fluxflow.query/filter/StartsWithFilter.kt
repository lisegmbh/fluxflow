package de.lise.fluxflow.query.filter

data class StartsWithFilter(
    val prefix: String,
    val ignoreCasing: Boolean
) : ValueFilter<String>

