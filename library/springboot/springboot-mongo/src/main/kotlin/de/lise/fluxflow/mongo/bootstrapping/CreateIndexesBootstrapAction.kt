package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.springframework.data.mongodb.core.MongoTemplate


class CreateIndexesBootstrapAction(
    mongoTemplate: MongoTemplate
) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        ensureIndex<WorkflowDocument>(
            "idx_model_type",
            false,
            WorkflowDocument::modelType
        )

        ensureIndex<StepDocument>(
            "idx_step_workflow_id",
            false,
            StepDocument::workflowId
        )

        ensureIndex<StepDocument>(
            "idx_step_kind",
            false,
            StepDocument::kind
        )

        ensureIndex<ContinuationRecordDocument>(
            "idx_continuation_record_workflow_id",
            false,
            ContinuationRecordDocument::workflowId
        )

        ensureIndex<ContinuationRecordDocument>(
            "idx_continuation_record_origin",
            false,
            ContinuationRecordDocument::originatingObject
        )

        ensureIndex<ContinuationRecordDocument>(
            "idx_continuation_record_type",
            false,
            ContinuationRecordDocument::type
        )

        ensureIndex<ContinuationRecordDocument>(
            "idx_continuation_record_target",
            false,
            ContinuationRecordDocument::targetObject
        )
    }
}

