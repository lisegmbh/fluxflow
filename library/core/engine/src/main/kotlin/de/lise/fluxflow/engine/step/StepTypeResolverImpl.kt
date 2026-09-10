package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import kotlin.reflect.KClass

class StepTypeResolverImpl(
    classLoader: ClassLoader,
    mappings: Map<StepKind, KClass<out Any>> = loadStepMappings(classLoader)
) : StepTypeResolver {
    private val mappings = mappings.toMap()

    constructor(classLoader: ClassLoader, typeRegistry: TypeRegistry) : this(
        classLoader,
        typeRegistry.toStepMappings(),
    )
    
    override fun resolveType(kind: StepKind): KClass<out Any> =
        mappings[kind] ?: throw UnknownTypeException(TypeRole.STEP, kind.value)
}

private fun loadStepMappings(classLoader: ClassLoader): Map<StepKind, KClass<out Any>> =
    TypeRegistry.load(classLoader).toStepMappings()

private fun TypeRegistry.toStepMappings(): Map<StepKind, KClass<out Any>> =
    entries
        .filter { it.role == TypeRole.STEP }
        .associate { StepKind(it.key) to it.type }
