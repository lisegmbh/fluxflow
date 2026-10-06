package de.lise.fluxflow.springboot.bootstrapping

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.query.JobQueryable.Companion.status
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.persistence.job.ScheduledJobReference
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.slf4j.LoggerFactory

class ReconcileScheduledJobsBootstrapAction private constructor(
    private val jobService: JobService,
    private val schedulingService: SchedulingService,
    private val source: Source,
) : BootstrapAction {
    constructor(
        jobService: JobService,
        schedulingService: SchedulingService,
        scheduledJobReferencePersistence: ScheduledJobReferencePersistence,
        workflowService: WorkflowService,
    ) : this(jobService, schedulingService, Source.References(scheduledJobReferencePersistence, workflowService))

    /** Legacy bulk reads retain their fail-fast behavior; use the four-argument route for isolation. */
    @Deprecated("Use the four-argument constructor for per-job reconciliation isolation")
    constructor(jobService: JobService, schedulingService: SchedulingService) :
        this(jobService, schedulingService, Source.Legacy)

    override fun setup() {
        Logger.info("Reconciling scheduled jobs on startup...")

        val references = source as? Source.References
        if (references == null) {
            jobService.findAll {
                where { status.isEqual(JobStatus.Scheduled) }
            }.items.forEach { job ->
                scheduleJobIfNeeded(ScheduledJobReference(job.workflow.identifier, job.identifier), job)
            }
            Logger.info("Scheduled job reconciliation completed.")
            return
        }
        val scheduledJobs = references.persistence.findScheduledJobReferences()

        if (scheduledJobs.isEmpty()) {
            Logger.info("No scheduled jobs found on startup.")
            return
        }

        scheduledJobs.forEach { reference ->
            reconcile(reference, references.workflows)
        }

        Logger.info("Scheduled job reconciliation completed.")
    }

    private fun reconcile(reference: ScheduledJobReference, workflowService: WorkflowService) {
        try {
            val workflow = workflowService.get<Any>(reference.workflowIdentifier)
            val job = jobService.getJob(workflow, reference.jobIdentifier)
            scheduleJobIfNeeded(reference, job)
        } catch (exception: Exception) {
            Logger.error(
                "Scheduled job reconciliation failed for workflow \"{}\" and job \"{}\".",
                reference.workflowIdentifier,
                reference.jobIdentifier,
                exception,
            )
        }
    }

    private fun scheduleJobIfNeeded(reference: ScheduledJobReference, job: Job) {
        val schedulingReference = SchedulingReference(
            reference.workflowIdentifier,
            reference.jobIdentifier,
            job.cancellationKey,
            null,
        )
        val isScheduled = schedulingService.isJobScheduled(
            schedulingReference
        )

        if (isScheduled) {
            return
        }

        Logger.warn(
            "Job with identifier \"{}\" is marked Scheduled but not found in scheduler. Rescheduling it.",
            reference.jobIdentifier,
        )

        schedulingService.schedule(
            job.scheduledTime,
            schedulingReference,
        )
    }

    companion object {
        private val Logger = LoggerFactory.getLogger(ReconcileScheduledJobsBootstrapAction::class.java)
    }

    private sealed interface Source {
        data object Legacy : Source
        data class References(val persistence: ScheduledJobReferencePersistence, val workflows: WorkflowService) : Source
    }
}
