package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.query.pagination.Page

interface StepService {
    fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>): List<Step>
    fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>, query: StepQuery): Page<Step>
    fun findSteps(query: StepQuery): Page<Step>
    
    fun <TWorkflowModel> findStep(workflow: Workflow<TWorkflowModel>, stepIdentifier: StepIdentifier): Step?

    /**
     * Sets a step's metadata entry for the given [key].
     * If the entry already exists, it will be overwritten.
     * Setting the [value] to `null` is going to remove the entry.
     * @param step The step that's metadata should be updated.
     * @param key The metadata entry's key.
     * @param value The value to be set. Specifying `null` will remove the entire entry.
     * @return The updated step.
     */
    fun setMetadata(step: Step, key: String, value: Any?): Step

    /**
     * Removes the specified [key] from the [step]'s metadata.
     * 
     * This is effectively an alias for calling [setMetadata] with a `null` value. 
     * @param step The step to be updated.
     * @param key The entry's key to be removed.
     * @return The updated step.
     */
    fun removeMetadata(step: Step, key: String): Step {
        return setMetadata(step, key, null)
    }
    
    fun reactivate(step: Step): Step
    fun complete(step: Step): Step
    fun cancel(step: Step): Step
}