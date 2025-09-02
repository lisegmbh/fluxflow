package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.InvokableStepDefinition
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepActivationContext
import de.lise.fluxflow.engine.step.data.RestoredData

class InvokableRestoredStepDefinition(
    override val definition: RestoredStepDefinition
) : InvokableStepDefinition {
    override fun activate(context: StepActivationContext): Step {
        val dataList = mutableListOf<RestoredData>()
        val step = RestoredStepImpl(
            context.stepIdentifier,
            definition.version,
            definition,
            context.workflow,
            context.status,
            context.metadata ?: definition.metadata,
            dataList
        )
        dataList.addAll(
            definition.data.map { it.createData(step) }
        )

        return step 
    }
}