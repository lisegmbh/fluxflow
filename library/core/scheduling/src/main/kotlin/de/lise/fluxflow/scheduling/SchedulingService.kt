package de.lise.fluxflow.scheduling

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

/**
 * The [SchedulingService] is responsible for scheduling an event and invoking a [SchedulingCallback] once it is due.
 */
interface SchedulingService {
    /**
     * Schedules a new event (referenced by [schedulingReference]) at the given time.
     * If the given [pointInTime] lies within the past or is now, implementations should invoke the listeners [SchedulingCallback.onScheduled] immediately.
     * @param pointInTime The time at which the event should be invoked.
     * @param schedulingReference A reference that allows the [SchedulingCallback] to identify the scheduled event.
     */
    fun schedule(
        pointInTime: Instant,
        schedulingReference: SchedulingReference
    )

    /**
     * Cancels all workflow jobs having the given cancellation key.
     * @param workflowIdentifier The owning workflow's identifier.
     * @param cancellationKey The key to be canceled.
     */
    fun cancel(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey)

    fun registerListener(callback: SchedulingCallback)
}