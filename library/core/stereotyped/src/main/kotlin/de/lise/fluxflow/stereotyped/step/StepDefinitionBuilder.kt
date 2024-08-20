package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.automation.Trigger
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.versioning.VersionBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

class StepDefinitionBuilder(
    private val versionBuilder: VersionBuilder,
    private val actionDefinitionBuilder: ActionDefinitionBuilder,
    private val dataDefinitionBuilder: DataDefinitionBuilder,
    private val metadataBuilder: MetadataBuilder,
    private val automationDefinitionBuilder: AutomationDefinitionBuilder,
    private val cache: MutableMap<KClass<*>, (element: Any) -> StepDefinition> = mutableMapOf()
) {
    fun <T : Any> build(element: T): StepDefinition {
        val type = element::class

        return cache.getOrPut(type) {
            createBuilder(type)
        }.invoke(element)
    }

    private fun <T : Any> createBuilder(type: KClass<out T>): (element: T) -> StepDefinition {
        val kind = StepKindInspector.getStepKind(type)
        val version = versionBuilder.build(type)

        val dataBuilders = dataDefinitionBuilder.buildDataDefinition(type)
        
        val hasExplicitActionAnnotation = actionDefinitionBuilder.hasAnnotatedAction(type)

        val automationFunctions = type.functions
            .filter { automationDefinitionBuilder.isAutomation(it) }
            .associateWith { automationDefinitionBuilder.build<T>(it) }

        val actionBuilders = type.functions
            .filter { !automationFunctions.containsKey(it) }
            .mapNotNull { actionDefinitionBuilder.build<T>(it, hasExplicitActionAnnotation) }

        return { instance ->
            ReflectedStatefulStepDefinition(
                instance,
                kind,
                version,
                dataBuilders.map { builder -> builder(instance) },
                actionBuilders.map { builder -> builder(instance) },
                metadataBuilder.build(type),
                automationFunctions.values
                    .mapNotNull { it[Trigger.OnCreated] }
                    .map { builder -> builder(instance) }
                    .toSet(),
                automationFunctions.values
                    .mapNotNull { it[Trigger.OnCompleted] }
                    .map { builder -> builder(instance) }
                    .toSet()
            )
        }
    }
}