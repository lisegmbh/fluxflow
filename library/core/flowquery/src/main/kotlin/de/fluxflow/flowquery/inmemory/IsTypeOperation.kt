package de.fluxflow.flowquery.inmemory

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

data class IsTypeOperation<TRoot>(
    val instanceGetter: InMemoryOperation<TRoot, Any?>,
    val requiredType: KClass<*>
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return instanceGetter.execute(input)?.let {
            it::class.isSubclassOf(requiredType)
        }
    }
}