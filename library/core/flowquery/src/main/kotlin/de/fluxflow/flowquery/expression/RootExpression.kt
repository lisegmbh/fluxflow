package de.fluxflow.flowquery.expression

import kotlin.reflect.KType

data class RootExpression<T>(
    override val resultType: KType
) : Expression<T, T> {
    override fun toText(): String {
        return "$"
    }

    override fun toString(): String {
        return toText()
    }
}