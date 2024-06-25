package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.RestoredStep
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.step.data.RestoredData

class RestoredStepImpl(
    override val identifier: StepIdentifier,
    override val definition: RestoredStepDefinition,
    override val workflow: Workflow<*>,
    override val status: Status,
    override val metadata: Map<String, Any>,
    override val data: List<RestoredData>,
) : StatefulStep, RestoredStep {
    override val actions: List<Action>
        get() = emptyList()
}