package de.lise.fluxflow.api.job.query.sort

import de.lise.fluxflow.query.sort.Sort
import java.time.Instant

data class JobScheduledTimeSort(
    val scheduledTimeSort: Sort<Instant>
) : JobSort
