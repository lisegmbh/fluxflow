package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.workflow.Workflow

class ReflectedStatefulStep(
    override val identifier: StepIdentifier,
    override val data: List<Data<*>>,
    override val actions: List<Action>,
    override val version: Version,
    override val definition: StepDefinition,
    override val workflow: Workflow<*>,
    override val status: Status,
    override val metadata: Map<String, Any>,
    val instance: Any
) : StatefulStep