package de.lise.fluxflow.api.job.query.sort

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.query.sort.Direction
import de.lise.fluxflow.query.sort.Sort
import java.time.Instant

interface JobSort : Sort<Job> {
    companion object {
        fun identifier(direction: Direction): JobSort {
            return JobIdSort(direction)
        }

        fun scheduledTime(sort: Sort<Instant>): JobSort {
            return JobScheduledTimeSort(sort)
        }
    }
}
