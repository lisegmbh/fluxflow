package de.lise.fluxflow.springboot.scheduling.quartz

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.springboot.scheduling.quartz.ext.jobIdentifier
import de.lise.fluxflow.springboot.scheduling.quartz.ext.workflowIdentifier
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean

class QuartzScheduledJob(
    private val quartzSchedulingService: QuartzSchedulingService
) : QuartzJobBean() {
    override fun executeInternal(context: JobExecutionContext) {
        val jobIdentifier = context.jobDetail?.jobIdentifier !!
        val workflowIdentifier = context.jobDetail?.workflowIdentifier!!
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