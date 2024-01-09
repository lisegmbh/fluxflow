package de.lise.fluxflow.api.step.query.sort

import de.lise.fluxflow.query.sort.Direction

data class StepIdSort(override val direction: Direction) : StepSort