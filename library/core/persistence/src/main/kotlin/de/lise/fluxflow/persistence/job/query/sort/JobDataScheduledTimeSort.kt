package de.lise.fluxflow.persistence.job.query.sort

import de.lise.fluxflow.api.job.query.sort.JobScheduledTimeSort
import de.lise.fluxflow.query.sort.Sort
import java.time.Instant

data class JobDataScheduledTimeSort(
    val sort: Sort<Instant>
) : JobDataSort {
    constructor(jobScheduledTimeSort: JobScheduledTimeSort) : this(
        jobScheduledTimeSort.scheduledTimeSort
    )
}
