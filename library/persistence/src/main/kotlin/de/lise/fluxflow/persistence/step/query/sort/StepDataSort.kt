package de.lise.fluxflow.persistence.step.query.sort

import de.lise.fluxflow.api.step.query.sort.StepIdSort
import de.lise.fluxflow.api.step.query.sort.StepSort
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.query.sort.Sort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface StepDataSort : Sort<StepData> {
    companion object {
        fun fromDomainSort(sort: StepSort): StepDataSort {
            return when (sort) {
                is StepIdSort -> StepDataIdSort(sort)
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}