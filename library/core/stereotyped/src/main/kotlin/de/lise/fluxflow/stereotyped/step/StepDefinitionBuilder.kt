package de.lise.fluxflow.stereotyped.step

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
    private val cache: MutableMap<KClass<*>, ReflectedStatefulStepDefinition> = mutableMapOf(),
) {
    fun <T : Any> build(type: KClass<T>): ReflectedStatefulStepDefinition {
        return cache.getOrPut(type) {
            createDefinition(type)
        }
    }

    fun <T : Any> createDefinition(type: KClass<T>): ReflectedStatefulStepDefinition {
        val kind = StepKindInspector.fromClass(type)
        val version = versionBuilder.build(type)

        val dataDefinitions = dataDefinitionBuilder.buildDataDefinition(type)

        val hasExplicitActionAnnotation = actionDefinitionBuilder.hasAnnotatedAction(type)

        val automationFunctions = type.functions
            .filter { automationDefinitionBuilder.isAutomation(it) }
            .associateWith { automationDefinitionBuilder.build<T>(it) }

        val actionDefinitions = type.functions
            .filter { !automationFunctions.containsKey(it) }
            .mapNotNull {
                actionDefinitionBuilder.build<T>(
                    it,
                    hasExplicitActionAnnotation
                )
            }

        return ReflectedStatefulStepDefinition(
            kind,
            version,
            dataDefinitions,
            actionDefinitions,
            metadataBuilder.build(type),
            automationFunctions.values
                .mapNotNull { it[Trigger.OnCreated] }
                .toSet(),
            automationFunctions.values
                .mapNotNull { it[Trigger.OnCompleted] }
                .toSet(),
            type,
        )
    }
}