package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class IsAnyOfOperator<TRoot, T>(
    val valueToTest: Expression<TRoot, T>,
    val anyOf: Set<Expression<*,T>>
): PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "${valueToTest.toText()} IN (${anyOf.joinToString(", "){ it.toText() }})"
    }

    override fun toString(): String {
        return toText()
    }
}