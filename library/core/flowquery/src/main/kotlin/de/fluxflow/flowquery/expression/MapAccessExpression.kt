package de.fluxflow.flowquery.expression

data class MapAccessExpression<TRoot, TCurrent: Map<TKey, TValue>, TKey, TValue>(
    val instance: Expression<TRoot, TCurrent>,
    val key: Expression<TRoot, TKey>
) : Expression<TRoot,TValue> {
    override fun toText(): String {
        return "${instance.toText()}[${key.toText()}]"
    }

    override fun toString(): String {
        return toText()
    }
}