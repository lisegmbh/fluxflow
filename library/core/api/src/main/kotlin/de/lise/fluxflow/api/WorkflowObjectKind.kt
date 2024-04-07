package de.lise.fluxflow.api

/**
 * Describes a workflow object's type.
 */
enum class WorkflowObjectKind {
    /**
     * The workflow object is a [de.lise.fluxflow.api.step.Step].
     */
    Step,

    /**
     * The workflow object is a [de.lise.fluxflow.api.job.Job]. 
     */
    Job,

    /**
     * The workflow object is a [de.lise.fluxflow.api.continuation.ContinuationRecord].
     */
    ContinuationRecord;
}