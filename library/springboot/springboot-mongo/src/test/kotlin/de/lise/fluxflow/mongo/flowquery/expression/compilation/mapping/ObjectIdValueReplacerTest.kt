package de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping

import de.fluxflow.flowquery.expression.ConstantExpression
import de.fluxflow.flowquery.expression.Expression
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.util.*

class ObjectIdValueReplacerTest {
    
    private val replacer = ObjectIdValueReplacer()

    @Test
    fun `replace should convert valid ObjectId string to ObjectId`() {
        // Arrange
        val expression = Expression.const<Any, String>("507f1f77bcf86cd799439011")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNotNull
        assertThat(result).isInstanceOf(ConstantExpression::class.java)
        val constantExpression = result as ConstantExpression<*, *>
        assertThat(constantExpression.value).isInstanceOf(ObjectId::class.java)
        assertThat((constantExpression.value as ObjectId).toHexString()).isEqualTo("507f1f77bcf86cd799439011")
    }

    @Test
    fun `replace should return null for invalid ObjectId string`() {
        // Arrange
        val expression = Expression.const<Any, String>("550e8400-e29b-41d4-a716-446655440000")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for UUID that is not valid ObjectId format`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val expression = Expression.const<Any, UUID>(uuid)

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for null constant`() {
        // Arrange
        val expression = Expression.const<Any, String?>(null)

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for non-string non-UUID constant`() {
        // Arrange
        val expression = Expression.const<Any, Int>(42)

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for already ObjectId constant`() {
        // Arrange
        val objectId = ObjectId("507f1f77bcf86cd799439011")
        val expression = Expression.const<Any, ObjectId>(objectId)

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should return null for non-constant expression`() {
        // Arrange
        val expression = Expression.root<TestDocument>()

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should handle empty string`() {
        // Arrange
        val expression = Expression.const<Any, String>("")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `replace should handle malformed ObjectId string`() {
        // Arrange
        val expression = Expression.const<Any, String>("not-an-objectid")

        // Act
        val result = replacer.replace(expression)

        // Assert
        assertThat(result).isNull()
    }

    data class TestDocument(
        val id: String,
        val name: String
    )
}

