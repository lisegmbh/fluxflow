package de.lise.fluxflow.engine.event.workflow

import de.lise.fluxflow.api.workflow.Workflow

/**
 * This event is raised immediately whenever FluxFlow is requested to persist changes to a workflow.
 * 
 * **Important**:
 * The event will be raised unconditionally every time a persist operation is **requested**.
 * This holds true,
 * even if FluxFlow later determines
 * that the persist operation can be skipped because the workflow didn't really change.
 */
@Deprecated("This event will not be raised in future versions of FluxFlow and is going to be removed.")
class BeforeWorkflowUpdateEvent(workflow: Workflow<*>): WorkflowEvent(workflow) {
    override val context: Any?
        get() = null
}