package de.lise.fluxflow.persistence.step.query.sort

import de.lise.fluxflow.api.step.query.sort.StepIdSort
import de.lise.fluxflow.query.sort.Direction

data class StepDataIdSort(val direction: Direction) : StepDataSort {
    constructor(idSort: StepIdSort) : this(idSort.direction)
}