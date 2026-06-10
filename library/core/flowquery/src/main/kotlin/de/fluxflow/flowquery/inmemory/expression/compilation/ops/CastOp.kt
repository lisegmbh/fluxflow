package de.fluxflow.flowquery.inmemory.expression.compilation.ops

import de.fluxflow.flowquery.expression.compilation.SubclassProvider
import kotlin.reflect.KClass

data class CastOp<TRoot, TTarget : Any>(
    private val instance: InMemoryOp<TRoot, Any?>,
    private val requiredType: KClass<TTarget>,
    private val subclassProvider: SubclassProvider,
) : InMemoryOp<TRoot, TTarget> {
    private val allowedTypes by lazy {
        subclassProvider.findSubclasses(requiredType)
    }

    override fun execute(input: TRoot): TTarget? {
        val value = instance.execute(input) ?: return null
        if (allowedTypes.none { it.isInstance(value) }) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return value as TTarget
    }
}
