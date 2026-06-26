package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class ContainsExpression<TRoot>(
    val value: Expression<TRoot, String>,
    val substring: Expression<TRoot, String>,
    val ignoreCasing: Boolean
) : PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "(${value.toText()}).contains(${substring.toText()}, ${
            when (ignoreCasing) {
                true -> "CASE_INSENSITIVE"
                false -> "CASE_SENSITIVE"
            }
        })"
    }

    override fun toString(): String {
        return toText()
    }
}