package de.fluxflow.flowquery.inmemory.expression.compilation.ops

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

data class IsTypeOp<TRoot>(
    val instanceGetter: InMemoryOp<TRoot, Any?>,
    val requiredType: KClass<*>
) : InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return instanceGetter.execute(input)?.let {
            it::class.isSubclassOf(requiredType)
        }
    }
}