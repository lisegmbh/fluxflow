package de.fluxflow.flowquery.expression

class Root<T> : Expression<T, T> {
    override fun asText(): String {
        return "$"
    }

    override fun toString(): String {
        return asText()
    }
}