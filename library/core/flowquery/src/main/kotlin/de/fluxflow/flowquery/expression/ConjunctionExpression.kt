package de.fluxflow.flowquery.expression

class ConjunctionExpression<TRoot, TCurrent> : Expression<TRoot, TCurrent> {
    override fun toText(): String {
        return "$"
    }

    override fun toString(): String {
        return toText()
    }

    override fun equals(other: Any?): Boolean {
        return other is ConjunctionExpression<*, *>
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}