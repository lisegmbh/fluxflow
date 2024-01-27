package de.lise.fluxflow.springboot.cache.memory.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.springboot.cache.memory.MemoryCache

class WorkflowCache : MemoryCache<WorkflowIdentifier, Workflow<*>>() {
    fun <TWorkflowModel> get(workflow: Workflow<TWorkflowModel>): Workflow<TWorkflowModel> {
        @Suppress("UNCHECKED_CAST")
        return get(workflow.identifier, workflow) as Workflow<TWorkflowModel>
    }
}