package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.StatefulStepImpl
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.workflow.Workflow

/**
 * The default implementation of [StatefulStepDefinition].
 */
class ReflectedStatefulStepDefinition(
    val instance: Any,
    override val kind: StepKind,
    override val data: List<DataDefinition<*>>,
    override val actions: List<ActionDefinition>,
    override val metadata: Map<String, Any>,
    override val onCreatedAutomations: Set<AutomationDefinition>,
    override val onCompletedAutomations: Set<AutomationDefinition>
) : StatefulStepDefinition {
    override fun createStep(
        workflow: Workflow<*>,
        stepIdentifier: StepIdentifier,
        status: Status
    ): Step {
        /* 
            We need to use a mutable list with deferred initialization,
            because of the circular dependency between StatefulStepImpl and its actions/data.
         */
        val dataList = mutableListOf<Data<*>>()
        val actionList = mutableListOf<Action>()

        val step = StatefulStepImpl(
            stepIdentifier,
            this,
            workflow,
            status,
            metadata,
            dataList,
            actionList,
        )

        dataList.addAll(0, data.map {
            it.createData(step)
        })

        actionList.addAll(0, actions.map {
            it.createAction(step)
        })

        return step
    }
}