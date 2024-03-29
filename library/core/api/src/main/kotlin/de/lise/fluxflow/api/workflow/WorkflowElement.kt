package de.lise.fluxflow.api.workflow

/**
 * Represents an element that can be part of a workflow.
 */
enum class WorkflowElement {
    Step,
    Job,
    ContinuationRecord;
}
