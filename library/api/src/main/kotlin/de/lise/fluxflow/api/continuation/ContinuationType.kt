package de.lise.fluxflow.api.continuation

/**
 * The [ContinuationType] describe a [Continuation]'s type.
 */
enum class ContinuationType {
    /**
     * A continuation created by [Continuation.none]. 
     */
    Nothing,

    /**
     * A continuation created by [Continuation.step].
     */
    Step,

    /**
     * A continuation created by [Continuation.job].
     */
    Job,

    /**
     * A continuation created by [Continuation.cancelJobs]
     */
    JobCancellation,

    /**
     * A continuation created by [Continuation.multiple]
     */
    Multiple,

    /**
     * A continuation created by [Continuation.rollback]
     */
    Rollback,

    /**
     * A continuation created by [Continuation.workflow]
     */
    Workflow

}