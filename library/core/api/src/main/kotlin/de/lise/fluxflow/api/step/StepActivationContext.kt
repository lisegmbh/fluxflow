package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.workflow.Workflow

/**
 * Holds information relevant for activating a workflow step based on its definition.
 * 
 * @property workflow The parent workflow the created step will be associated to.
 * @property stepIdentifier The identifier for the step to be created.
 * @property version The version that has been used to initially create the step.
 * @property status The status the step to be created should be in.
 * @property metadata The metadata that should be associated with the step.
 * If `null`, the definition's metadata should be applied.
 */
data class StepActivationContext(
    val workflow: Workflow<*>,
    val stepIdentifier: StepIdentifier,
    val version: Version,
    val status: Status,
    val metadata: Map<String, Any>?,
)