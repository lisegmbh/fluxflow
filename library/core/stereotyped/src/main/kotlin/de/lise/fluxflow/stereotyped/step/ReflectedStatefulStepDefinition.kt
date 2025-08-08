package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.versioning.Version
import kotlin.reflect.KClass

/**
 * The default implementation of [StatefulStepDefinition].
 */
class ReflectedStatefulStepDefinition(
    override val kind: StepKind,
    override val version: Version,
    override val data: List<DataDefinition<*>>,
    override val actions: List<ActionDefinition>,
    override val metadata: Map<String, Any>,
    override val onCreatedAutomations: Set<AutomationDefinition>,
    override val onCompletedAutomations: Set<AutomationDefinition>,
    val backingType: KClass<*>,
) : StatefulStepDefinition {

    fun toInvokableStepDefinition(instance: Any): InvokableReflectedStepDefinition {
        return InvokableReflectedStepDefinition(
            this,
            instance
        )
    }
}

