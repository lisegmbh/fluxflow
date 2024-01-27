package de.lise.fluxflow.api.workflow

/**
 * Represents a workflow.
 * 
 * @param TModel The type of the data model storing all relevant workflow information.
 */
interface Workflow<TModel> {
    /**
     * Returns this workflow's unique identifier. 
     */
    val identifier: WorkflowIdentifier

    /**
     * An alias for [identifier].
     */
    @Deprecated(
        "This is an alias for .identifier and will be removed in future versions.",
        replaceWith = ReplaceWith("identifier")
    )
    val id: WorkflowIdentifier
        get() { return identifier }

    /**
     * Returns the data associated with this workflow.
     */
    val model: TModel
}
