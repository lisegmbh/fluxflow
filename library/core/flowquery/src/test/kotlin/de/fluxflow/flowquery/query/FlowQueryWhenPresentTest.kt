package de.fluxflow.flowquery.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowQueryWhenPresentTest {

    @Test
    fun `whenPresent should not change query for null values`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val result = query.whenPresent(null as String?) { value ->
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(result).isSameAs(query)
        assertThat(result.operations).isEmpty()
    }

    @Test
    fun `whenPresent should not change query for blank strings`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val result = query.whenPresent("   ") { value ->
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(result).isSameAs(query)
        assertThat(result.operations).isEmpty()
    }

    @Test
    fun `whenPresent should not change query for empty collections, maps and arrays`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val collectionResult = query.whenPresent(emptyList<String>()) { value ->
            get(TestModel::tags).isAnyOf(value)
        }
        val mapResult = query.whenPresent(emptyMap<String, String>()) { value ->
            get(TestModel::metadata).isEqual(value)
        }
        val arrayResult = query.whenPresent(emptyArray<String>()) { value ->
            get(TestModel::aliases).isEqual(value.toList())
        }

        // Assert
        assertThat(collectionResult).isSameAs(query)
        assertThat(mapResult).isSameAs(query)
        assertThat(arrayResult).isSameAs(query)
        assertThat(query.operations).isEmpty()
    }

    @Test
    fun `whenPresent should add a filter for present values`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val result = query.whenPresent("Cologne") { value ->
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(result).isNotSameAs(query)
        assertThat(result.operations).hasSize(1)
        assertThat(result.operations.first()).isInstanceOf(FilterOperation::class.java)
    }

    @Test
    fun `whenPresent should pass non-null value to builder`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()
        var received: String? = null

        // Act
        query.whenPresent("ACTIVE") { value ->
            received = value
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(received).isEqualTo("ACTIVE")
    }

    data class TestModel(
        val name: String,
        val tags: List<String>,
        val metadata: Map<String, String>,
        val aliases: List<String>,
    )
}
