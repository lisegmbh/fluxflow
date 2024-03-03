package de.lise.fluxflow.api.workflow.continuation

/**
 * The fork behavior controls what should happen to the original workflow,
 * whenever it starts a new workflow using the [WorkflowContinuation].
 */
enum class ForkBehavior {
    /**
     * This behavior will create a new workflow and continues the original workflow normally.
     */
    Fork,

    /**
     * This behavior will remove the original workflow once the new workflow is started.  
     */
    Remove,

    /***
     * This behavior will remove the original workflow and create the new workflow reusing the old identifier.
     */
    Replace
}