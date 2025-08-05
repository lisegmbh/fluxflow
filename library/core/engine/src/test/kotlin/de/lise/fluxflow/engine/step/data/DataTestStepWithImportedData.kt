package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.stereotyped.Import

class DataTestStepWithImportedData(
    @Import
    val data: ImportableData 
)