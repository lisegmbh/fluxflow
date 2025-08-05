package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.stereotyped.step.data.Data

data class ImportableData(
    @Data
    val firstname: String,
    @Data
    var lastname: String
)