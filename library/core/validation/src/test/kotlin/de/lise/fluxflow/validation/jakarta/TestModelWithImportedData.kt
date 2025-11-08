package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.stereotyped.Import

data class TestModelWithImportedData(
    @Import
    val data: ImportedData
)