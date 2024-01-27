package de.lise.fluxflow.springboot.cache.memory.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.WorkflowStepIdentifier
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.step.StepActivationService
import de.lise.fluxflow.persistence.step.StepData
import java.util.*

class MemoryCachingStepActivationService(
    private val stepActivationService: StepActivationService,
    private val stepCache: StepCache
) : StepActivationService {
    private val definitionCache = WeakHashMap<Any, StepDefinition>()

    override fun <TWorkflowModel> activate(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData
    ): Step {
        return stepCache.get(
            WorkflowStepIdentifier(
                workflow.identifier,
                StepIdentifier(stepData.id)
            )
        ) {
            stepActivationService.activate(workflow, stepData)
        }
    }

    override fun toStepDefinition(definitionObject: Any): StepDefinition {
        synchronized(definitionCache) {
            val cachedValue = definitionCache[definitionObject]
            if(cachedValue != null) {
                return cachedValue
            }
            val newValue = stepActivationService.toStepDefinition(definitionObject)
            definitionCache[definitionObject] = newValue
            return newValue
        }
    }

}