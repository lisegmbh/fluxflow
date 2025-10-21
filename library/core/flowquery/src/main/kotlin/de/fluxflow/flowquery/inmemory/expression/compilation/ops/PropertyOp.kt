package de.fluxflow.flowquery.inmemory.expression.compilation.ops

import kotlin.reflect.KProperty1

internal data class PropertyOp<TRoot>(
    val instance: InMemoryOp<TRoot, Any?>,
    val property: KProperty1<Any, Any?>
): InMemoryOp<TRoot, Any?> {
    override fun execute(input: TRoot): Any? {
        val currentInstance = instance.execute(input)
        if(currentInstance == null) {
            return null
        }
        return property.get(currentInstance)
    }

    override fun toString(): String {
        return "$instance.${property.name}"
    }
}