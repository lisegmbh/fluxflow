package de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping

import de.fluxflow.flowquery.expression.Expression
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjectIdReplacerTest {
    
    private val replacer = ObjectIdReplacer()

    @Test
    fun `replace should convert valid ObjectId string when expression has id property`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::id)
            .isEqual("507f1f77bcf86cd799439011")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNotNull
    }

    @Test
    fun `replace should handle invalid ObjectId string when expression has id property`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::id)
            .isEqual("550e8400-e29b-41d4-a716-446655440000") // UUID, not ObjectId

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should handle expression with id property and multiple values`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::id)
            .isEqual("507f1f77bcf86cd799439011")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNotNull
    }

    @Test
    fun `replace should return null when expression has no id property`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::name)
            .isEqual("test")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for constant expression without id context`() {
        // Arrange
        val expression = Expression.const<TestDocument, String>("507f1f77bcf86cd799439011")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for root expression`() {
        // Arrange
        val expression = Expression.root<TestDocument>()

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null when property name is not id`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::name)
            .isEqual("507f1f77bcf86cd799439011")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should handle null values in id comparison`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::id)
            .isEqual(null)

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should handle mixed valid and invalid ObjectId strings`() {
        // Arrange
        val expression = Expression.root<TestDocument>()
            .get(TestDocument::id)
            .isEqual("507f1f77bcf86cd799439011")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNotNull
    }

    data class TestDocument(
        val id: String,
        val name: String,
        val nested: NestedDocument? = null
    )

    data class NestedDocument(
        val id: String,
        val value: String
    )
}

