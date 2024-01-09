package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.query.pagination.Page

interface StepService {
    fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>): List<Step>
    fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>, query: StepQuery): Page<Step>

    fun <TWorkflowModel> findStep(workflow: Workflow<TWorkflowModel>, stepIdentifier: StepIdentifier): Step?

    fun reactivate(step: Step): Step
    fun complete(step: Step): Step
    fun cancel(step: Step): Step
}