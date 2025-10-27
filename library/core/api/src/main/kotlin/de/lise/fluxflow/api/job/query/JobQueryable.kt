package de.lise.fluxflow.api.job.query

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

interface JobQueryable {
    val identifier: JobIdentifier
    val workflowIdentifier: WorkflowIdentifier
    val kind: JobKind
    val parameters: Map<String, Any?>
    val scheduledTime: Instant
    val cancellationKey: String?
    val status: JobStatus

    companion object {
        val <TRoot> Expression<TRoot, JobQueryable>.identifier
            get() = this.get(JobQueryable::identifier)

        val <TRoot> Expression<TRoot, JobQueryable>.workflowIdentifier
            get() = this.get(JobQueryable::workflowIdentifier)

        val <TRoot> Expression<TRoot, JobQueryable>.kind
            get() = this.get(JobQueryable::kind)

        val <TRoot> Expression<TRoot, JobQueryable>.parameters
            get() = this.get(JobQueryable::parameters)

        val <TRoot> Expression<TRoot, JobQueryable>.scheduledTime
            get() = this.get(JobQueryable::scheduledTime)

        val <TRoot> Expression<TRoot, JobQueryable>.cancellationKey
            get() = this.get(JobQueryable::cancellationKey)

        val <TRoot> Expression<TRoot, JobQueryable>.status
            get() = this.get(JobQueryable::status)
    }
}