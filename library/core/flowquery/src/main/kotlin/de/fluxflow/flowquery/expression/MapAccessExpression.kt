package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class MapAccessExpression<TRoot, TCurrent: Map<TKey, TValue>, TKey, TValue>(
    val instance: Expression<TRoot, TCurrent>,
    val key: Expression<TRoot, TKey>
) : Expression<TRoot,TValue> {
    override val resultType: KType =
        instance.resultType.arguments[1].type ?: typeOf<Any?>()

    override fun toText(): String {
        return "${instance.toText()}[${key.toText()}]"
    }

    override fun toString(): String {
        return toText()
    }
}