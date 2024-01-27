package de.lise.fluxflow.engine.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AssumingChangeDetectorTest {
    @Test
    fun `hasChanged should always return true if assumed result is true`() {
        // Arrange
        val changeDetector = AssumingChangeDetector<Any?>(true)
        val element = Any()
        val other = Any()

        // Act
        val equalElementsResult = changeDetector.hasChanged(element, element)
        val differentElementsResult = changeDetector.hasChanged(element, other)

        // Assert
        assertThat(equalElementsResult).isTrue()
        assertThat(differentElementsResult).isTrue()
    }

    @Test
    fun `hasChanged should always return false if assumed result is false`() {
        // Arrange
        val changeDetector = AssumingChangeDetector<Any?>(false)
        val element = Any()
        val other = Any()

        // Act
        val equalElementsResult = changeDetector.hasChanged(element, element)
        val differentElementsResult = changeDetector.hasChanged(element, other)

        // Assert
        assertThat(equalElementsResult).isFalse()
        assertThat(differentElementsResult).isFalse()
    }
}