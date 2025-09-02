package de.lise.fluxflow.springboot.scheduling.quartz

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.springboot.scheduling.quartz.ext.jobIdentifier
import de.lise.fluxflow.springboot.scheduling.quartz.ext.toJobData
import de.lise.fluxflow.springboot.scheduling.quartz.ext.toKey
import de.lise.fluxflow.springboot.scheduling.quartz.ext.workflowIdentifier
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import java.time.Clock
import java.time.Instant
import java.util.*

/**
 * Quartz-based implementation of [SchedulingService].
 *
 * This service uses Quartz to schedule and manage workloads.
 *
 * @param scheduler The fully initialized Quartz scheduler.
 * @param clock The clock instance used to obtain the current time.
 * @param enableLegacyLookup If `true`, enables the legacy lookup for scheduled jobs.
 * In earlier implementations, a deterministic Quartz job key was not assigned
 * when no cancellation key was provided.
 * This caused inefficient lookups,
 * as the service had to iterate over all scheduled jobs to find the matching job data.
 */
open class QuartzSchedulingService(
    private val scheduler: Scheduler,
    private val clock: Clock,
    private val enableLegacyLookup: Boolean,
) : SchedulingService {
    private val listeners: MutableList<SchedulingCallback> = mutableListOf()

    protected open fun createJobMetadata(
        pointInTime: Instant,
        schedulingReference: SchedulingReference,
    ): JobDetail {
        return JobBuilder
            .newJob(QuartzScheduledJob::class.java)
            .usingJobData(schedulingReference.toJobData())
            .withIdentity(schedulingReference.toKey())
            .build()
    }

    protected open fun createTrigger(
        pointInTime: Instant,
        schedulingReference: SchedulingReference,
    ): Trigger {
        return TriggerBuilder.newTrigger()
            .startAt(Date.from(pointInTime))
            .build()
    }

    override fun schedule(
        pointInTime: Instant,
        schedulingReference: SchedulingReference,
    ) {
        if (pointInTime <= clock.instant()) {
            return notify(schedulingReference)
        }
        val job = createJobMetadata(
            pointInTime,
            schedulingReference
        )
        val trigger = createTrigger(
            pointInTime,
            schedulingReference
        )

        scheduler.scheduleJob(
            job,
            trigger
        )
    }

    override fun cancel(
        workflowIdentifier: WorkflowIdentifier,
        cancellationKey: CancellationKey,
    ) {
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

    fun getJobDetail(
        schedulingRef: SchedulingReference,
    ): JobDetail? {
        val detailsFromKey = scheduler.getJobDetail(
            schedulingRef.toKey()
        )
        if (detailsFromKey != null || !enableLegacyLookup) {
            return detailsFromKey
        }

        // Otherwise, search for it by searching within the job data
        for (existingKey in scheduler.getJobKeys(GroupMatcher.anyGroup())) {
            val details = scheduler.getJobDetail(existingKey)
            if (
                details.jobIdentifier == schedulingRef.jobIdentifier &&
                details.workflowIdentifier == schedulingRef.workflowIdentifier
            ) {
                return details
            }
        }

        return null
    }

    override fun isJobScheduled(
        schedulingReference: SchedulingReference,
    ): Boolean {
        return getJobDetail(schedulingReference) != null
    }
}

