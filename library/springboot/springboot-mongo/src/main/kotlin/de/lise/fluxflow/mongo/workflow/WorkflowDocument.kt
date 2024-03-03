package de.lise.fluxflow.mongo.workflow

import de.lise.fluxflow.persistence.workflow.WorkflowData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class WorkflowDocument(
    @Id var id: String?,
    var model: Any?,
    var modelType: String?
) {
    fun toWorkflowData(): WorkflowData {
        return WorkflowData(
            id!!,
            model
        )
    }
}