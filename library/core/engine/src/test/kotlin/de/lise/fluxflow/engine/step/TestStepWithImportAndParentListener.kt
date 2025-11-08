package de.lise.fluxflow.engine.step

import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.data.DataListener

@Step
class TestStepWithImportAndParentListener(
    @Import
    val importedInformation: TestDataToBeImported,
    var parentListenerTriggered: Boolean = false
) {
    @DataListener("name")
    fun onImportedNameUpdate(oldValue: String?, newValue: String?) {
        parentListenerTriggered = true
    }
}