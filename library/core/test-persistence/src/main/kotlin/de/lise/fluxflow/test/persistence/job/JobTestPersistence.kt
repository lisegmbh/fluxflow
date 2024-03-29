package de.lise.fluxflow.test.persistence.job

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.test.persistence.TestIdGenerator

class JobTestPersistence(
    private val idGenerator: TestIdGenerator = TestIdGenerator(),
    private val entities: MutableMap<String, JobData> = mutableMapOf()
) : JobPersistence {
    override fun randomId(): String {
        return idGenerator.newId()
    }

    override fun create(jobData: JobData): JobData {
        return save(jobData)
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<JobData> {
        return entities.values.filter {
            it.workflowId == workflowIdentifier.value
        }
    }

    override fun cancelJobs(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey) {
        entities.replaceAll { _, element ->
            if (
                element.workflowId != workflowIdentifier.value
                || element.cancellationKey != cancellationKey.value
                || element.status != JobStatus.Scheduled
            ) {
                element
            } else {
                element.withStatus(JobStatus.Canceled)
            }
        }
    }

    override fun findForWorkflowAndId(workflowIdentifier: WorkflowIdentifier, jobIdentifier: JobIdentifier): JobData? {
        return entities[jobIdentifier.value]
            ?.takeIf { it.workflowId == workflowIdentifier.value }
    }

    override fun save(jobData: JobData): JobData {
        entities[jobData.id] = jobData
        return jobData
    }

    override fun deleteAllForWorkflow(workflowIdentifier: WorkflowIdentifier) {
        entities.entries.removeAll { (_, v) -> v.workflowId == workflowIdentifier.value }
    }
}
