package de.lise.fluxflow.engine.step

import de.lise.fluxflow.stereotyped.step.data.DataListener

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