package de.lise.fluxflow.reflection.activation.parameter

class DelayedParameterProvider<T> : ParameterProvider<T> {
    private var value: (() -> T)? = null
    override fun provide(): T {
        return (value ?: throw NullPointerException("Delayed value has not been initialized."))()
    }

    fun set(value: T) {
        this.value = {
            value
        }
    }
}