package de.lise.fluxflow.springboot.activation

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.StepConfigurationException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRegistrationConflictException
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.springboot.types.FluxFlowTypeRegistryFactory
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * Builds the registered mapping from [StepKind] to step class.
 */
class StepKindMapBuilder @JvmOverloads constructor(
    private val context: ApplicationContext,
    private val classLoader: ClassLoader,
    private val typeRegistry: TypeRegistry? = null,
) {
    fun build(): Map<StepKind, KClass<out Any>> = try {
        val contextRegistry = if (typeRegistry == null && context.getBeansOfType(TypeRegistry::class.java).isNotEmpty()) {
            context.getBean(TypeRegistry::class.java)
        } else null
        val registry = typeRegistry ?: contextRegistry ?: FluxFlowTypeRegistryFactory(
            context,
            classLoader,
        ).create()
        registry.entries
            .filter { it.role == TypeRole.STEP }
            .associate { StepKind(it.key) to it.type }
    } catch (exception: TypeRegistrationConflictException) {
        if (exception.role != TypeRole.STEP) throw exception
        throw StepConfigurationException(exception.message!!).apply { initCause(exception) }
    }
}
