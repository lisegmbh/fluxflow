package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.StepKind
import kotlin.reflect.KClass

class StepTypeResolverImpl(
    private val classLoader: ClassLoader,
    mappings: Map<StepKind, KClass<out Any>> = emptyMap()
) : StepTypeResolver {
    private val cache = mappings.toMutableMap()
    
    override fun resolveType(kind: StepKind): KClass<out Any> {
        if(cache.containsKey(kind)) {
            return cache[kind]!!
        }
        val result = Class.forName(kind.value, true, classLoader).kotlin
        cache[kind] = result
        return result
    }
}