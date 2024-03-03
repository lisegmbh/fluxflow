package de.lise.fluxflow.test.persistence.step.query.sort

import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.query.sort.StepDataIdSort
import de.lise.fluxflow.persistence.step.query.sort.StepDataSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryDefaultSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryPropertySort
import de.lise.fluxflow.query.inmemory.sort.InMemorySort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface StepTestSort {
    companion object {
        fun toTestSort(
            sort: StepDataSort
        ): InMemorySort<StepData> {
            return when (sort) {
                is StepDataIdSort -> InMemoryPropertySort(
                    StepData::id,
                    InMemoryDefaultSort(sort.direction)
                )

                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}