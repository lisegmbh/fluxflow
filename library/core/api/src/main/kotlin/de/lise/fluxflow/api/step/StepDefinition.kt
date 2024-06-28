package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.workflow.Workflow

/**
 * The [StepDefinition] holds information on a step.
 * It mainly defines how a step should be executed and managed by the workflow engine.
 */
interface StepDefinition {
    val kind: StepKind

    /**
     * This [StepDefinition]'s version.
     * Incompatible versions are usually caused by breaking changes to a step's definition.
     */
    val version: Version

    /**
     * A simple key-value map providing additional metadata about this step definition and steps that are going to be produced by it.
     */
    val metadata: Map<String, Any>

    /**
     * A set of automations to be run after the step has been created.
     */
    val onCreatedAutomations: Set<AutomationDefinition>

    /**
     * A set of automations to be run after the step transitioned into the [Status.Completed] status.
     */
    val onCompletedAutomations: Set<AutomationDefinition>

    /**
     * Creates a new step based on this definition.
     *
     * @param workflow The parent workflow the created step will be associated to.
     * @param stepIdentifier The identifier for the step to be created.
     * @param version The version that has been used to initially create the step.
     * @param status The status the step to be created should be in.
     * @param metadata The metadata that should be associated with the step. 
     * If `null`, the definition's metadata should be applied. 
     */
    fun createStep(
        workflow: Workflow<*>,
        stepIdentifier: StepIdentifier,
        version: Version,
        status: Status,
        metadata: Map<String, Any>?
    ): Step
}