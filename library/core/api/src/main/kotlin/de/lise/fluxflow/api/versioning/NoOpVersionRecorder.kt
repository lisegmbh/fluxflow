package de.lise.fluxflow.api.versioning

class NoOpVersionRecorder<in TWorkflowElement> : VersionRecorder<TWorkflowElement> {
    override fun record(element: TWorkflowElement) {
        // As this recorder's purpose is to provide a no-operation implementation,
        // the implementation is intentionally left blank.
    }
}