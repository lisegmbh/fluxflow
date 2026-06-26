package de.fluxflow.flowquery.expression

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class IsTypeExpression<TRoot, TCurrent, TType: Any>(
    val instance: Expression<TRoot, TCurrent>,
    val requiredType: KClass<TType>
) : PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "${instance.toText()} IS ${requiredType.qualifiedName}"
    }

    override fun toString(): String {
        return toText()
    }
}