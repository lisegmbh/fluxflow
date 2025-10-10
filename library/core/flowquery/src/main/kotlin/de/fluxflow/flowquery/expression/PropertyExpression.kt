package de.fluxflow.flowquery.expression

import kotlin.reflect.KProperty1

class PropertyExpression<TRoot, TInstance, TProperty>(
    val instance: Expression<TRoot, TInstance>,
    val property: KProperty1<TInstance, TProperty?>
) : Expression<TRoot, TProperty> {
    override fun asText(): String {
        return "${instance.asText()}.${property.name}"
    }

    override fun toString(): String {
        return asText()
    }
}