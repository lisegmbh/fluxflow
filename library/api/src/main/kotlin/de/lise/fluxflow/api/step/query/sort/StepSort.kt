package de.lise.fluxflow.api.step.query.sort

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.query.sort.Direction
import de.lise.fluxflow.query.sort.Sort

interface StepSort : Sort<Step> {
    companion object {
        fun id(direction: Direction): StepSort {
            return StepIdSort(direction)
        }
    }
}

