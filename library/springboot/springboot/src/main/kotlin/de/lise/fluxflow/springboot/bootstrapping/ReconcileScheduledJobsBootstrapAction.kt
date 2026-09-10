package de.lise.fluxflow.springboot.bootstrapping

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.persistence.job.ScheduledJobReference
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.slf4j.LoggerFactory

class ReconcileScheduledJobsBootstrapAction(
    private val jobService: JobService,
    private val schedulingService: SchedulingService,
    private val scheduledJobReferencePersistence: ScheduledJobReferencePersistence,
    private val workflowService: WorkflowService,
) : BootstrapAction {
    override fun setup() {
        Logger.info("Reconciling scheduled jobs on startup...")

        val scheduledJobs = scheduledJobReferencePersistence.findScheduledJobReferences()

        if (scheduledJobs.isEmpty()) {
            Logger.info("No scheduled jobs found on startup.")
            return
        }

        scheduledJobs.forEach { reference ->
            reconcile(reference)
        }

        Logger.info("Scheduled job reconciliation completed.")
    }

    private fun reconcile(reference: ScheduledJobReference) {
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
}
