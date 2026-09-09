package de.lise.fluxflow.springboot.activation

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.springboot.types.FluxFlowTypeRegistryFactory
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * Builds the registered mapping from [StepKind] to step class.
 */
class StepKindMapBuilder(
    private val context: ApplicationContext,
    private val classLoader: ClassLoader,
    private val typeRegistry: TypeRegistry? = null,
) {
    fun build(): Map<StepKind, KClass<out Any>> {
        val registry = typeRegistry ?: FluxFlowTypeRegistryFactory(
            context,
            classLoader,
        ).create()
        return registry.entries
            .filter { it.role == TypeRole.STEP }
            .associate { StepKind(it.key) to it.type }
    }
}
