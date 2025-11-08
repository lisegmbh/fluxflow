package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.stereotyped.step.data.Data
import jakarta.validation.constraints.NotBlank

data class ImportedData(
    @Data
    @field:NotBlank
    val someProperty: String
)