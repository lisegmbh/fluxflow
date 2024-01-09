package de.lise.fluxflow.api.step.stateful

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepImpl
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.workflow.Workflow

class StatefulStepImpl(
    identifier: StepIdentifier,
    definition: StatefulStepDefinition,
    workflow: Workflow<*>,
    status: Status,
    metadata: Map<String, Any>,
    override val data: List<Data<*>>,
    override val actions: List<Action>
) : StepImpl(
    identifier,
    definition,
    workflow,
    status,
    metadata
), StatefulStep