package de.fluxflow.flowquery.expression

class HasKeyExpression<TRoot, TKey>(
    val instance: Expression<TRoot, out Map<TKey, *>>,
    val key: Expression<TRoot, TKey>
) : LogicalExpression<TRoot> {
    override fun toText(): String {
        return "${instance.toText()} HAS PROPERTY ${key.toText()}"
    }
}