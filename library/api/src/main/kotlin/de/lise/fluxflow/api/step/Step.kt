package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.workflow.Workflow

/**
 * The [Step] API element represents a step within a workflow.
 *
 * Known subtypes are:
 * - [StatefulStep]: Used for steps that are stateful (e.g., having step data, exposing actions).
 */
interface Step {
    /**
     * The unique identifier that can be used to identify this step.
     *
     * **Note that** the identifier is only guaranteed to be unique within the executing workflow.
     * It is possible that different workflows are having steps with the same identifier.
     */
    val identifier: StepIdentifier

    /**
     * The [StepDefinition] describing this step.
     */
    val definition: StepDefinition

    /**
     * The workflow this step belongs to.
     */
    val workflow: Workflow<*>

    /**
     * This step's current status.
     */
    val status: Status

    /**
     * Arbitrary metainformation associated with this step.
     */
    val metadata: Map<String, Any>
}

