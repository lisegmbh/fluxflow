package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

data class IsTypeOp<TRoot>(
    val instanceGetter: InMemoryOperation<TRoot, Any?>,
    val requiredType: KClass<*>
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return instanceGetter.execute(input)?.let {
            it::class.isSubclassOf(requiredType)
        }
    }
}