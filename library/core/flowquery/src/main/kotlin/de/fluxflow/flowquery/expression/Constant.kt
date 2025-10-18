package de.fluxflow.flowquery.expression

data class Constant<TRoot, T>(
    val value: T
): Expression<TRoot, T> {
    override fun toText(): String {
        return "$value"
    }

    override fun toString(): String {
        return toText()
    }
}