package de.lise.fluxflow.mongo.job

import de.lise.fluxflow.mongo.query.sort.MongoDefaultSort
import de.lise.fluxflow.mongo.query.sort.MongoSort
import de.lise.fluxflow.persistence.job.query.sort.JobDataIdSort
import de.lise.fluxflow.persistence.job.query.sort.JobDataScheduledTimeSort
import de.lise.fluxflow.persistence.job.query.sort.JobDataSort
import de.lise.fluxflow.query.sort.UnsupportedSortException
import org.springframework.data.domain.Sort

interface JobDocumentSort {
    companion object {
        fun toMongoSort(
            sort: JobDataSort
        ): Sort {
            return when (sort) {
                is JobDataIdSort -> MongoDefaultSort<String>(sort.direction).apply("id")
                is JobDataScheduledTimeSort -> MongoSort.toMongoSort(sort.sort).apply("scheduledTime")
                else -> throw UnsupportedSortException(sort)

            }
        }
    }
}
