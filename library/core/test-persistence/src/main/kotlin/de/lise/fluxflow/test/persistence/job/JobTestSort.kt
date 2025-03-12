package de.lise.fluxflow.test.persistence.job

import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.query.sort.JobDataIdSort
import de.lise.fluxflow.persistence.job.query.sort.JobDataScheduledTimeSort
import de.lise.fluxflow.persistence.job.query.sort.JobDataSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryDefaultSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryPropertySort
import de.lise.fluxflow.query.inmemory.sort.InMemorySort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface JobTestSort {
    companion object {
        fun toTestSort(
            sort: JobDataSort
        ): InMemorySort<JobData> {
            return when (sort) {
                is JobDataIdSort -> InMemoryPropertySort(
                    JobData::id,
                    InMemoryDefaultSort(sort.direction)
                )

                is JobDataScheduledTimeSort -> InMemoryPropertySort(
                    JobData::scheduledTime,
                    InMemorySort.toTestSort(sort.sort)
                )

                else -> throw UnsupportedSortException(sort)
            }
        }
    }

}
