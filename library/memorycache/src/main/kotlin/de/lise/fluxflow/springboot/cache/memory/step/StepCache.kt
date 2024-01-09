package de.lise.fluxflow.springboot.cache.memory.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.WorkflowStepIdentifier
import de.lise.fluxflow.springboot.cache.memory.MemoryCache

class StepCache : MemoryCache<WorkflowStepIdentifier, Step>() {
    fun get(step: Step): Step {
        return get(
            WorkflowStepIdentifier(step),
            step
        )
    }
}