package de.fluxflow.flowquery.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowQueryIfPresentTest {

    @Test
    fun `ifPresent should not change query for null values`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val result = query.ifPresent(null as String?) { value ->
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(result).isSameAs(query)
        assertThat(result.operations).isEmpty()
    }

    @Test
    fun `ifPresent should not change query for blank strings`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val result = query.ifPresent("   ") { value ->
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(result).isSameAs(query)
        assertThat(result.operations).isEmpty()
    }

    @Test
    fun `ifPresent should not change query for empty collections, maps and arrays`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val collectionResult = query.ifPresent(emptyList<String>()) { value ->
            get(TestModel::tags).isAnyOf(value)
        }
        val mapResult = query.ifPresent(emptyMap<String, String>()) { value ->
            get(TestModel::metadata).isEqual(value)
        }
        val arrayResult = query.ifPresent(emptyArray<String>()) { value ->
            get(TestModel::aliases).isEqual(value.toList())
        }

        // Assert
        assertThat(collectionResult).isSameAs(query)
        assertThat(mapResult).isSameAs(query)
        assertThat(arrayResult).isSameAs(query)
        assertThat(query.operations).isEmpty()
    }

    @Test
    fun `ifPresent should add a filter for present values`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()

        // Act
        val result = query.ifPresent("Cologne") { value ->
            get(TestModel::name).isEqual(value)
        }

        // Assert
        assertThat(result).isNotSameAs(query)
        assertThat(result.operations).hasSize(1)
        assertThat(result.operations.first()).isInstanceOf(FilterOperation::class.java)
    }

    @Test
    fun `ifPresent should pass non-null value to builder`() {
        // Arrange
        val query = FlowQuery.of<TestModel>()
        var received: String? = null

        // Act
        query.ifPresent("ACTIVE") { value ->
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
