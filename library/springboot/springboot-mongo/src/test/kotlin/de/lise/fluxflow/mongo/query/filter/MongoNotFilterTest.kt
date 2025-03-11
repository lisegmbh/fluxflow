package de.lise.fluxflow.mongo.query.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.bson.Document

import org.junit.jupiter.api.Test
class MongoNotFilterTest {
    @Test
    fun `not filter should negate inner filter`() {
        // Arrange
        val innerFilter = MongoEqualFilter("NotExpectedValue")
        val notFilter = MongoNotFilter(innerFilter)

        // Act
        val criteria = notFilter.apply("path")

        // Assert
        val innerCriteria = innerFilter.apply("path").criteriaObject
        val expectedDocument = Document("\$nor", listOf(innerCriteria))
        assertEquals(expectedDocument, criteria.criteriaObject)
    }
}