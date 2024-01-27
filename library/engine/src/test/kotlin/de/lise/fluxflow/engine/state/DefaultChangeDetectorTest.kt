package de.lise.fluxflow.engine.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DefaultChangeDetectorTest {
    @Test
    fun `hasChanged should use the objects equality to determine if an object has changed`() {
        // Arrange
        val equalOther = Any()
        val unequalOther = Any()
        val element = TestObject(equalOther)
        val changeDetector = DefaultChangeDetector<Any>()

        // Act & Assert
        val equalElementsResult = changeDetector.hasChanged(element, equalOther)
        assertThat(equalElementsResult).isFalse()
        assertThat(element.hasBeenComparedTo).containsExactly(equalOther)
        element.clear()

        val unequalElementsResult = changeDetector.hasChanged(element, unequalOther)
        assertThat(unequalElementsResult).isTrue()
        assertThat(element.hasBeenComparedTo).containsExactly(unequalOther)
    }

    @Suppress("EqualsOrHashCode")
    private class TestObject (
        private val expectedEqual: Any
    ){

        val hasBeenComparedTo: MutableList<Any?> = mutableListOf()

        fun clear() {
            hasBeenComparedTo.clear()
        }

        override fun equals(other: Any?): Boolean {
            hasBeenComparedTo.add(other)
            if(other === expectedEqual) {
                return true
            }
            return super.equals(other)
        }
    }
}