package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.StepKind

data class StepDataKind(
    val stepKind: StepKind,
    val dataKind: DataKind
)
