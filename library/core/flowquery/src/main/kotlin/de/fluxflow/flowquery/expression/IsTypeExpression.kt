package de.fluxflow.flowquery.expression

import kotlin.reflect.KClass

data class IsTypeExpression<TRoot, TCurrent, TType: Any>(
    val instance: Expression<TRoot, TCurrent>,
    val requiredType: KClass<TType>
) : FlowPredicate<TRoot> {
    override fun toText(): String {
        return "${instance.toText()} IS ${requiredType.qualifiedName}"
    }

    override fun toString(): String {
        return toText()
    }
}