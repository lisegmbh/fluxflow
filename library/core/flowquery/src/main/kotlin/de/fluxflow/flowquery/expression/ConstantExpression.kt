package de.fluxflow.flowquery.expression

import kotlin.reflect.KType

data class ConstantExpression<TRoot, T>(
    override val resultType: KType,
    val value: T
): Expression<TRoot, T> {
    override fun toText(): String {
        return "$value"
    }

    override fun toString(): String {
        return toText()
    }
}