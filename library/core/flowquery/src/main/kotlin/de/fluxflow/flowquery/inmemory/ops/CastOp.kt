package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation
import kotlin.reflect.KClass

data class CastOp<TRoot, TCurrent, TTarget : Any>(
    private val instance: InMemoryOperation<TRoot, TCurrent>,
    private val requiredType: KClass<TTarget>
) : InMemoryOperation<TRoot, TTarget> {
    override fun execute(input: TRoot): TTarget? {
        return instance.execute(input) as? TTarget?
    }
}