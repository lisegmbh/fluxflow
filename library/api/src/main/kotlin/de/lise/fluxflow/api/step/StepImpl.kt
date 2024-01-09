package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.workflow.Workflow

open class StepImpl(
    override val identifier: StepIdentifier,
    override val definition: StepDefinition,
    override val workflow: Workflow<*>,
    override val status: Status,
    override val metadata: Map<String, Any>
): Step {

}