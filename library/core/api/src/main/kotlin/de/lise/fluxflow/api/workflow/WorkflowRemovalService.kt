package de.lise.fluxflow.api.workflow

interface WorkflowRemovalService {
    /**
     * Deletes the workflow with the given identifier and all of its related steps and jobs.
     *
     * Nota that you do **not** need to cancel any scheduled jobs, as this is done automatically by FluxFlow.
     *
     * @param identifierToDelete the id of the workflow to delete
     */
    fun delete(identifierToDelete: WorkflowIdentifier)
}