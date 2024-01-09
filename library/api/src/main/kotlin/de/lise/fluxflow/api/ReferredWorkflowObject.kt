package de.lise.fluxflow.api

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.step.Step

/**
 * The [ReferredWorkflowObject] is similar to [WorkflowObjectReference], but additionally contains the referred object itself.
 * This allows for immediate access to the said object, without having to refetch it.
 */
class ReferredWorkflowObject<T> private constructor(
    val targetObject: T,
    val reference: WorkflowObjectReference
) {
    /**
     * A convenience property returning the referred step if this reference is indeed targeting a step.
     * Might be `null`, if this reference does not refer to a step.
     */
    val step: Step?
        get() = targetObject as? Step

    /**
     * A convenience property returning the referred job if this reference is indeed targeting a job.
     * Might be `null`, if this reference does not refer to a job.
     */
    val job: Job?
        get() = targetObject as? Job
    
    companion object {
        fun create(step: Step): ReferredWorkflowObject<Step> {
            return ReferredWorkflowObject(
                step,
                WorkflowObjectReference.create(step)
            )
        }
        
        fun create(job: Job): ReferredWorkflowObject<Job> {
            return ReferredWorkflowObject(
                job,
                WorkflowObjectReference.create(job)
            )
        }
    }
}