package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.step.data.DataListener

class DataTestStepWithImportedData(
    @Import
    val data: ImportableData 
) {
    var onChangeInvoked = false
    
    @DataListener("lastname")
    fun onChange() {
        onChangeInvoked = true    
    }
}