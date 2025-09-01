package de.lise.fluxflow.springboot.scheduling.quartz.ext

import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.springboot.scheduling.quartz.JobDataMapKeys
import org.quartz.JobDataMap
import org.quartz.JobKey

fun SchedulingReference.toKey(): JobKey {
    return this.cancellationKey?.let {
        JobKey(
            it.value,
            this.workflowIdentifier.value
        )
    }?: JobKey(
        this.jobIdentifier.value,
        this.workflowIdentifier.value
    )
}

fun SchedulingReference.toJobData(): JobDataMap {
    return JobDataMap(mapOf(
        JobDataMapKeys.JOB_IDENTIFIER to this.jobIdentifier.value,
        JobDataMapKeys.WORKFLOW_IDENTIFIER to this.workflowIdentifier.value
    ))
}