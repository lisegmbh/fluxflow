package de.lise.fluxflow.springboot.scheduling.quartz

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import java.time.Clock
import java.time.Instant
import java.util.*

open class QuartzSchedulingService(
    private val scheduler: Scheduler,
    private val clock: Clock
) : SchedulingService {
    private val listeners: MutableList<SchedulingCallback> = mutableListOf()

    protected open fun createJobMetadata(
        pointInTime: Instant,
        schedulingReference: SchedulingReference
    ): JobDetail {
        var builder = JobBuilder
            .newJob(QuartzScheduledJob::class.java)

            .usingJobData(
                "jobIdentifier", schedulingReference.jobIdentifier.value
            ).usingJobData(
                "workflowIdentifier", schedulingReference.workflowIdentifier.value
            )

        schedulingReference.cancellationKey?.let {
            builder = builder.withIdentity(
                it.value,
                schedulingReference.workflowIdentifier.value
            )
        }

        return builder.build()
    }

    protected open fun createTrigger(
        pointInTime: Instant,
        schedulingReference: SchedulingReference
    ): Trigger {
        return TriggerBuilder.newTrigger()
            .startAt(Date.from(pointInTime))
            .build()
    }

    override fun schedule(pointInTime: Instant, schedulingReference: SchedulingReference) {
        if (pointInTime <= clock.instant()) {
            return notify(schedulingReference)
        }
        val job = createJobMetadata(pointInTime, schedulingReference)
        val trigger = createTrigger(pointInTime, schedulingReference)

        scheduler.scheduleJob(job, trigger)
    }

    override fun cancel(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey) {
        scheduler.deleteJob(
            JobKey.jobKey(
                cancellationKey.value,
                workflowIdentifier.value
            )
        )
    }

    override fun registerListener(callback: SchedulingCallback) {
        listeners.add(callback)
    }

    fun notify(ref: SchedulingReference) {
        listeners.forEach { listener ->
            listener.onScheduled(ref)
        }
    }

    override fun isJobScheduled(
        schedulingReference: SchedulingReference
    ): Boolean {
        return getScheduledJobKeys().any { jobKey ->
            val job = scheduler.getJobDetail(jobKey) ?: return@any false
            val jobId = job.jobDataMap["jobIdentifier"] as? String

            jobId == schedulingReference.jobIdentifier.value
        }
    }

    private fun getScheduledJobKeys(): Set<JobKey> = scheduler.getJobKeys(GroupMatcher.anyGroup()).toSet()
}

