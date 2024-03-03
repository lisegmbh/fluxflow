package de.lise.fluxflow.engine.continuation

class StepFinalizationSemaphore(private var isFinalized: Boolean) {

    fun finalizeStep(consumer: () -> Unit) {
        synchronized(this) {
            if (isFinalized) {
                return
            }
            isFinalized = true
        }
        consumer()
    }
}