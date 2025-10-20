package de.fluxflow.flowquery.inmemory

import kotlin.reflect.KClass

data class CastOperation<TRoot, TCurrent, TTarget : Any>(
    private val instance: InMemoryOperation<TRoot, TCurrent>,
    private val requiredType: KClass<TTarget>
) : InMemoryOperation<TRoot, TTarget> {
    override fun execute(input: TRoot): TTarget? {
        return instance.execute(input) as? TTarget?
    }
}