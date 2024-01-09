package de.lise.fluxflow.api

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.step.Step

/**
 * A [WorkflowObjectReference] can point to any arbitrary workflow object.
 */
data class WorkflowObjectReference(
    /**
     * The kind of the workflow object this reference is referring to.
     */
    val kind: WorkflowObjectKind,
    /**
     * The identifier of the workflow object this reference is referring to.
     */
    val objectId: String
) {
    companion object {
        fun create(step: Step): WorkflowObjectReference {
            return WorkflowObjectReference(
                WorkflowObjectKind.Step,
                step.identifier.value,
            )
        }
        
        fun create(job: Job): WorkflowObjectReference {
            return WorkflowObjectReference(
                WorkflowObjectKind.Job,
                job.identifier.value
            )
        }
    }
}

