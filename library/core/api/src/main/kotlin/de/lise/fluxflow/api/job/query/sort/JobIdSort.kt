package de.lise.fluxflow.api.job.query.sort

import de.lise.fluxflow.query.sort.Direction

data class JobIdSort(override val direction: Direction) : JobSort
