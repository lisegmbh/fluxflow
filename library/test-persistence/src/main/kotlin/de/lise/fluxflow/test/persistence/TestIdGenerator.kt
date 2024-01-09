package de.lise.fluxflow.test.persistence

class TestIdGenerator(
    private var index: Int = 0
) {
    private val lockObject = Any()
    fun newId(): String {
        synchronized(lockObject) {
            return "${index++}"
        }
    }
}