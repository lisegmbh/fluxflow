package de.lise.fluxflow.engine.reflection

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EqualsAndHashCodeBuilderTest {
    @Test
    fun `objects without properties should be considered equal`() {
        // Arrange
        val object1 = EmptyModel()
        val object2 = EmptyModel()
        val equalsAndHashCode = EqualsAndHashCode.forType<EmptyModel>()

        // Act
        val areEqual = equalsAndHashCode.areEqual(object1, object2)

        // Assert
        assertThat(areEqual).isTrue()
    }

    @Test
    fun `objects without properties should always produce the same hash`() {
        // Arrange
        val object1 = EmptyModel()
        val object2 = EmptyModel()
        val equalsAndHashCode = EqualsAndHashCode.forType<EmptyModel>()

        // Act
        val hash1 = equalsAndHashCode.composeHash(object1)
        val hash2 = equalsAndHashCode.composeHash(object2)

        // Assert
        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun `objects having the same properties should be considered equal`() {
        // Arrange
        val object1 = ModelWithProps("a", 1, null)
        val object2 = ModelWithProps("a", 1, null)
        val equalsAndHashCode = EqualsAndHashCode.forType<ModelWithProps>(true)

        // Act
        val areEqual = equalsAndHashCode.areEqual(object1, object2)

        // Assert
        assertThat(areEqual).isTrue()
    }

    @Test
    fun `objects having the same properties should produce the same hash`() {
        // Arrange
        val object1 = ModelWithProps("a", 1, null)
        val object2 = ModelWithProps("a", 1, null)
        val equalsAndHashCode = EqualsAndHashCode.forType<ModelWithProps>(true)

        // Act
        val hash1 = equalsAndHashCode.composeHash(object1)
        val hash2 = equalsAndHashCode.composeHash(object2)

        // Assert
        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun `objects having different properties should be considered different`() {
        // Arrange
        val object1 = ModelWithProps("a", 1, null)
        val object2 = ModelWithProps("a", 2, null)
        val equalsAndHashCode = EqualsAndHashCode.forType<ModelWithProps>(true)

        // Act
        val areEqual = equalsAndHashCode.areEqual(object1, object2)

        // Assert
        assertThat(areEqual).isFalse()
    }

    @Test
    fun `objects having different properties should produce different hashes`() {
        // Arrange
        val object1 = ModelWithProps("a", 1, null)
        val object2 = ModelWithProps("b", 1, null)
        val equalsAndHashCode = EqualsAndHashCode.forType<ModelWithProps>(true)

        // Act
        val hash1 = equalsAndHashCode.composeHash(object1)
        val hash2 = equalsAndHashCode.composeHash(object2)

        // Assert
        assertThat(hash1).isNotEqualTo(hash2)
    }

    private class EmptyModel
    private class ModelWithProps(
        val someProperty: String,
        val anotherProperty: Int,
        val complexProperty: Any?
    )
}