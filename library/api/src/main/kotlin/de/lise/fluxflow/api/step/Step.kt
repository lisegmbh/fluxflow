package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.workflow.Workflow

interface Step {
    val identifier: StepIdentifier
    val definition: StepDefinition
    val workflow: Workflow<*>
    val status: Status
    val metadata: Map<String, Any>
}

