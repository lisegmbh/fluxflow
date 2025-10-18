package de.fluxflow.flowquery.expression

class Constant<TRoot, T>(
    val value: T
): Expression<TRoot, T> {
    override fun toText(): String {
        return "$value"
    }

    override fun toString(): String {
        return toText()
    }
}