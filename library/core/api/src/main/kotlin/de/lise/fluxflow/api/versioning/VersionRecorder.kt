package de.lise.fluxflow.api.versioning

/**
 * A version recorder is used to save a [TWorkflowDefinition]'s version.
 */
fun interface VersionRecorder<in TWorkflowDefinition> {
    /**
     * Records the current version of the given [element].
     */
    fun record(element: TWorkflowDefinition)
}