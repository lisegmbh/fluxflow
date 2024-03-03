package de.lise.fluxflow.api.workflow

interface WorkflowUpdateService {
    fun <TWorkflowModel> saveChanges(
        workflow: Workflow<TWorkflowModel>
    ): Workflow<TWorkflowModel>
}