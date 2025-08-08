package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.InvokableStepDefinition
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepActivationContext
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.data.Data

class InvokableReflectedStepDefinition(
    override val definition: ReflectedStatefulStepDefinition,
    private val backingInstance: Any,
) : InvokableStepDefinition {
    override fun activate(context: StepActivationContext): Step {
        /* 
           We need to use a mutable list with deferred initialization,
           because of the circular dependency between StatefulStepImpl and its actions/data.
        */
        val dataList = mutableListOf<Data<*>>()
        val actionList = mutableListOf<Action>()

        val step = ReflectedStatefulStep(
            identifier = context.stepIdentifier,
            version = context.version,
            definition = definition,
            workflow = context.workflow,
            status = context.status,
            metadata = context.metadata ?: definition.metadata,
            data = dataList,
            actions = actionList,
            instance = backingInstance
        )

        dataList.addAll(
            0,
            definition.data.map {
                it.createData(step)
            })

        actionList.addAll(
            0,
            definition.actions.map {
                it.createAction(step)
            })

        return step
    }
}