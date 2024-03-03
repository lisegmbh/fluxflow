package de.lise.fluxflow.query.filter

data class ContainsFilter(
    val content: String,
    val ignoreCasing: Boolean
) : Filter<String>