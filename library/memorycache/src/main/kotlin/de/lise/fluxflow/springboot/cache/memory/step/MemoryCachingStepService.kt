package de.lise.fluxflow.springboot.cache.memory.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.WorkflowStepIdentifier
import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.query.pagination.Page

class MemoryCachingStepService(
    private val stepService: StepService,
    private val stepCache: StepCache
) : StepService {
    override fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>): List<Step> {
        return stepService.findSteps(workflow)
            .map { stepCache.get(it) }
    }

    override fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>, query: StepQuery): Page<Step> {
        return stepService.findSteps(
            workflow,
            query
        ).map {
            stepCache.get(it)
        }
    }

    override fun <TWorkflowModel> findStep(workflow: Workflow<TWorkflowModel>, stepIdentifier: StepIdentifier): Step? {
        return try {
            stepCache.get(
                WorkflowStepIdentifier(workflow.identifier, stepIdentifier)
            ) {
                stepService.findStep(workflow, stepIdentifier) ?: throw NoSuchElementException()
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    override fun reactivate(step: Step): Step {
        return stepService.reactivate(step)
    }

    override fun complete(step: Step): Step {
        return stepService.complete(step)
    }

    override fun cancel(step: Step): Step {
        return stepService.cancel(step)
    }
}