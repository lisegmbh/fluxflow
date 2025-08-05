package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingReference
import org.slf4j.LoggerFactory

class JobSchedulingCallback(
    private val workflowQueryService: WorkflowQueryService,
    private val workflowUpdateService: WorkflowUpdateService,
    private val jobService: JobServiceImpl,
    private val continuationService: ContinuationService
) : SchedulingCallback {

    private companion object {
        val Logger = LoggerFactory.getLogger(JobSchedulingCallback::class.java)!!
    }

    override fun onScheduled(ref: SchedulingReference) {
        Logger.debug(
            "Scheduled job \"{}\" is about to be executed.",
            ref
        )
        
        val workflow = ref.alreadyLoadedJob?.workflow ?: try {
            workflowQueryService.get<Any>(ref.workflowIdentifier)
        } catch (e: WorkflowNotFoundException) {
            Logger.warn(
                "The workflow \"{}\" that owned job \"{}\" seems to be lost. Skipping job execution.",
                ref.workflowIdentifier,
                ref,
                e
            )
            return
        }

        val job = ref.alreadyLoadedJob ?: jobService.getJobOrNull(workflow, ref.jobIdentifier)
        if (job == null) {
            Logger.warn("Job definition for scheduled job \"{}\" seems to be lost.", ref)
            return
        }

        val runningJob = jobService.setStatus(job, JobStatus.Running)
        val continuation: Continuation<*>
        try {
            continuation = runningJob.execute()
        } catch(e: Throwable) {
            Logger.error("Workflow job {} failed during execution.", ref, e)
            jobService.setStatus(runningJob, JobStatus.Failed)
            return
        }
        jobService.setStatus(runningJob, JobStatus.Executed)
        @Suppress("UNCHECKED_CAST")
        workflowUpdateService.saveChanges(workflow as Workflow<Any>)
        continuationService.execute(
            workflow, 
            ReferredWorkflowObject.create(runningJob),
            continuation
        )
    }
}