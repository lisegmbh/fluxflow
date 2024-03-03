package de.lise.fluxflow.springboot.scheduling.quartz

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.scheduling.SchedulingReference
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean

class QuartzScheduledJob(
    private val quartzSchedulingService: QuartzSchedulingService
) : QuartzJobBean() {
    override fun executeInternal(context: JobExecutionContext) {
        val jobIdentifier = JobIdentifier(
            context.jobDetail!!.jobDataMap!!["jobIdentifier"]!! as String
        )
        val workflowIdentifier = WorkflowIdentifier(
            context.jobDetail!!.jobDataMap!!["workflowIdentifier"]!! as String
        )
        val cancellationKey = context.jobDetail!!.key?.name?.let {
            CancellationKey(
                it
            )
        }
        quartzSchedulingService.notify(
            SchedulingReference(
                workflowIdentifier,
                jobIdentifier,
                cancellationKey,
                null,
            )
        )
    }
}