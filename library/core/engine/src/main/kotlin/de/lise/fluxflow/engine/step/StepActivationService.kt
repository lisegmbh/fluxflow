package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.InvokableStepDefinition
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.step.StepData

interface StepActivationService {
    fun <TWorkflowModel> activateInitial(
        workflow: Workflow<TWorkflowModel>,
        invokableStepDefinition: InvokableStepDefinition,
        identifier: StepIdentifier
    ): Step
    fun <TWorkflowModel> activateFromPersistence(workflow: Workflow<TWorkflowModel>, stepData: StepData): Step
    fun toStepDefinition(definitionObject: Any): StepDefinition
    fun toInvokableStepDefinition(definitionObject: Any): InvokableStepDefinition
}