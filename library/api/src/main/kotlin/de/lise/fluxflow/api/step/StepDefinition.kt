package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.api.workflow.Workflow

/**
 * The [StepDefinition] holds information on a step.
 * It mainly defines how a step should be executed and managed by the workflow engine.
 */
interface StepDefinition {
    val kind: StepKind

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
     */
    fun createStep(
        workflow: Workflow<*>,
        stepIdentifier: StepIdentifier,
        status: Status
    ): Step
}