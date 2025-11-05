package de.lise.fluxflow.engine.step

import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.data.DataListener

@Step
class TestStepWithImport(
    @Import
    val importedInformation: TestDataToBeImported 
)

data class TestDataToBeImported(
    var name: String,
) {
    
    var nameWasUpdatedToArthurDent: Boolean? = null
        private set
    
    @DataListener("name")
    fun onNameUpdate(oldValue: String, newValue: String) {
        nameWasUpdatedToArthurDent = newValue == "Arthur Dent"
    }
}