package de.lise.fluxflow.mongo.bootstrapping

internal class ActionBuffer<T>(
    private val bufferSize: Int,
    private val callback: (bufferedElements: List<T>) -> Unit
) {
    private val buffer = mutableListOf<T>()

    fun push(element: T) {
        synchronized(buffer) {
            buffer.add(element)
            if(buffer.size >= bufferSize) {
                flush()
            }
        }
    }

    fun flush() {
        synchronized(buffer) {
            callback(buffer)
            buffer.clear()
        }
    }
}