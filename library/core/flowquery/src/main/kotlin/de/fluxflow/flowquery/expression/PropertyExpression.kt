package de.fluxflow.flowquery.expression

import kotlin.reflect.KProperty1
import kotlin.reflect.KType

data class PropertyExpression<TRoot, TInstance, TProperty>(
    val instance: Expression<TRoot, TInstance>,
    val property: KProperty1<TInstance, TProperty?>
) : Expression<TRoot, TProperty> {
    override val resultType: KType = property.returnType

    override fun toText(): String {
        return "${instance.toText()}.${property.name}"
    }

    override fun toString(): String {
        return toText()
    }
}