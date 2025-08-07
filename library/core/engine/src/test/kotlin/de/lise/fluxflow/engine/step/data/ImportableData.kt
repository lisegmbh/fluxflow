package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.stereotyped.step.data.Data
import de.lise.fluxflow.stereotyped.step.data.DataListener

data class ImportableData(
    @Data
    val firstname: String,
    @Data
    var lastname: String
) {
    
    var onChangeInvoked = false
    var givenWorkflow: Workflow<*>? = null
    var givenStep: Step? = null
    
    @DataListener("lastname")
    fun onLastnameChange(
        workflow: Workflow<*>,
        step: Step
    ) {
        onChangeInvoked = true
        givenWorkflow = workflow
        givenStep = step
    }
    
}