package de.fluxflow.flowquery.inmemory.expression.compilation.ops

import kotlin.reflect.KClass

data class CastOp<TRoot, TCurrent, TTarget : Any>(
    private val instance: InMemoryOp<TRoot, TCurrent>,
    private val requiredType: KClass<TTarget>
) : InMemoryOp<TRoot, TTarget> {
    override fun execute(input: TRoot): TTarget? {
        return instance.execute(input) as? TTarget?
    }
}