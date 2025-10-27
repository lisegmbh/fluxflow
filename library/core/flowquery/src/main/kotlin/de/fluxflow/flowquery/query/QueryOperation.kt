package de.fluxflow.flowquery.query

sealed interface QueryOperation {
    fun toText(): String
}

