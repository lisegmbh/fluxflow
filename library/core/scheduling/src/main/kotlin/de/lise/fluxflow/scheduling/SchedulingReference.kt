package de.lise.fluxflow.scheduling

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

/**
 * A scheduling reference is a way to track scheduled events.
 */
data class SchedulingReference(
    val workflowIdentifier: WorkflowIdentifier,
    val jobIdentifier: JobIdentifier,
    val cancellationKey: CancellationKey?,
    val alreadyLoadedJob: Job?,
) {
    override fun toString(): String {
        return "$workflowIdentifier/$jobIdentifier"
    }
}
