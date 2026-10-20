package de.fluxflow.flowquery.expression

import kotlin.reflect.KClass

data class CastExpression<TRoot, TCurrent: Any, TTarget : TCurrent>(
    val instance: Expression<TRoot, TCurrent>,
    val requiredType: KClass<TTarget>
): Expression<TRoot, TTarget> {
    override fun toText(): String {
        return "${instance.toText()} AS ${requiredType.qualifiedName}"
    }
}