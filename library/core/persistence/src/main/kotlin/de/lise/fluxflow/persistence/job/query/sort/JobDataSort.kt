package de.lise.fluxflow.persistence.job.query.sort

import de.lise.fluxflow.api.job.query.sort.JobIdSort
import de.lise.fluxflow.api.job.query.sort.JobScheduledTimeSort
import de.lise.fluxflow.api.job.query.sort.JobSort
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.query.sort.Sort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface JobDataSort : Sort<JobData> {
    companion object {
        fun fromDomainSort(sort: JobSort): JobDataSort {
            return when (sort) {
                is JobIdSort -> JobDataIdSort(sort)
                is JobScheduledTimeSort -> JobDataScheduledTimeSort(sort)
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}