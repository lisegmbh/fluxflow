package de.fluxflow.flowquery.expression

import kotlin.reflect.KType

class ConjunctionExpression<TRoot, TCurrent>(
    override val resultType: KType,
) : Expression<TRoot, TCurrent> {
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