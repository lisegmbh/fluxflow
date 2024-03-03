package de.lise.fluxflow.engine.step.automation

import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.stereotyped.step.automation.OnCreated

class TestWorkflowStepWithAutomationFunctionExpectingParameters {
    var receivedWorkflowStarterService: WorkflowStarterService? = null
    
    @OnCreated
    fun expectedWorkflowService(workflowStarterService: WorkflowStarterService) {
        receivedWorkflowStarterService = workflowStarterService
    }
}