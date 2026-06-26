package de.fluxflow.flowquery.expression

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

data class CastExpression<TRoot, TCurrent: Any, TTarget : TCurrent>(
    val instance: Expression<TRoot, TCurrent>,
    val requiredType: KClass<TTarget>
): Expression<TRoot, TTarget> {
    override val resultType: KType
        get() = requiredType.createType(
            arguments = requiredType.typeParameters.map { KTypeProjection.STAR },
            nullable = false
        )

    override fun toText(): String {
        return "${instance.toText()} AS ${requiredType.qualifiedName}"
    }
}