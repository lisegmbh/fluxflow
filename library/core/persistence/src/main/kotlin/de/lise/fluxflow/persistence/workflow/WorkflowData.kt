package de.lise.fluxflow.persistence.workflow

data class WorkflowData(
    val id: String,
    val model: Any?
) {
    fun <TWorkflowModel> withModel(model: TWorkflowModel): WorkflowData {
        return WorkflowData(
            id,
            model
        )
    }
}