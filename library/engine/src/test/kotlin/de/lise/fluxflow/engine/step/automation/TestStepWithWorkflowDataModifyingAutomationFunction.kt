package de.lise.fluxflow.engine.step.automation

import de.lise.fluxflow.stereotyped.step.automation.OnCreated

class TestStepWithWorkflowDataModifyingAutomationFunction(
    private val workflowModel: TestWorkflowModelForModifyingAutomationFunctions
) {
    @OnCreated
    fun run() {
        workflowModel.hasBeenChanged = true
    }
}