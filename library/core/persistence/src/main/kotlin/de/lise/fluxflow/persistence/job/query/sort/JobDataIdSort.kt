package de.lise.fluxflow.persistence.job.query.sort

import de.lise.fluxflow.api.job.query.sort.JobIdSort
import de.lise.fluxflow.query.sort.Direction

data class JobDataIdSort(override val direction: Direction) : JobDataSort {
    constructor(idSort: JobIdSort) : this(idSort.direction)
}
