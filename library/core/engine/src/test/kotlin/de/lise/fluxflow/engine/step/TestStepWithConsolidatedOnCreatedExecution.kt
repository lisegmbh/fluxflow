package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.stereotyped.step.automation.OnCreated

class TestStepWithConsolidatedOnCreatedExecution{
    var steps: Collection<Step> = emptySet()
    
    @OnCreated
    fun doWork(
        workflow: Workflow<*>,
        stepService: StepService    
    ) {
        steps = stepService.findSteps(workflow)
    }
}