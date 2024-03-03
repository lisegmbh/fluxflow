package de.lise.fluxflow.scheduling

/**
 * A [SchedulingCallback] is the receiver for scheduled events.
 */
fun interface SchedulingCallback {
    /**
     * This function will be invoked by the scheduler to signal, that the schedule time has come.
     * @param ref The scheduling reference as specified when the event has originally been scheduled.
     */
    fun onScheduled(ref: SchedulingReference)
}