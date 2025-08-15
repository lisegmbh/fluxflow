package de.lise.fluxflow.springboot.bootstrapping

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.query.filter.JobFilter
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.slf4j.LoggerFactory

class ReconcileScheduledJobsBootstrapAction(
    private val jobService: JobService,
    private val schedulingService: SchedulingService,
) : BootstrapAction {
    override fun setup() {
        Logger.info("Reconciling scheduled jobs on startup...")

        val scheduledJobs = jobService.findAll(
            Query.withFilter(
                JobFilter.empty().withStatus(
                    Filter.eq(JobStatus.Scheduled)
                )
            )
        )

        if (scheduledJobs.items.isEmpty()) {
            Logger.info("No scheduled jobs found on startup.")
            return
        }

        scheduledJobs.items.forEach { job ->
            scheduleJobIfNeeded(job)
        }

        Logger.info("Scheduled job reconciliation completed.")
    }

    private fun scheduleJobIfNeeded(job: Job) {
        val schedulingReference = SchedulingReference(
            job.workflow.identifier,
            job.identifier,
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
            job.identifier
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