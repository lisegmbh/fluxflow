package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class StartsWithExpression<TRoot>(
    val value: Expression<TRoot, String>,
    val prefix: Expression<TRoot, String>,
    val ignoreCasing: Boolean
) : PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "(${value.toText()}).startsWith(${prefix.toText()}, ${
            when(ignoreCasing){ 
                true -> "CASE_INSENSITIVE" 
                false -> "CASE_SENSITIVE" 
            }
        })"
    }

    override fun toString(): String {
        return toText()
    }
}