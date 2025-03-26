package de.lise.fluxflow.mongo.query.filter

import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.query.Criteria

class MongoElemMatchFilterTest {
    @Test
    fun `elemMatch filter should invoke the nested value filter using the path given by elemMatch`() {
        // Arrange
        val nestedValueFilter = mock<MongoFilter<Int>> {
            on { apply(any()) } doReturn Criteria()
        }
        val mongoElemMatchFilter = MongoElemMatchFilter(
            String::length,
            nestedValueFilter
        )

        // Act
        mongoElemMatchFilter.apply("prefixes")

        // Assert
        verify(nestedValueFilter, times(1)).apply(eq("length"))
    }

    data class TestDocument(
        @Field(value = "renamedProperty")
        val property: String
    )
    @Test
    fun `elemMatch filter should consider field names overwritten by annotation`() {
        // Arrange
        val nestedValueFilter = mock<MongoFilter<String>> {
            on { apply(any()) } doReturn Criteria()
        }
        val mongoElemMatchFilter = MongoElemMatchFilter(
            TestDocument::property,
            nestedValueFilter
        )

        // Act
        mongoElemMatchFilter.apply("prefixes")

        // Assert
        verify(nestedValueFilter, times(1)).apply(eq("renamedProperty"))
    }

    @Test
    fun `elemMatch filter uses correct opertaion to apply nested filter`() {
        // Arrange
        val nestedFilter = MongoEqualFilter(2)
        val mongoElemMatchFilter = MongoElemMatchFilter(
            String::length,
            nestedFilter
        )

        // Act
        val criteria = mongoElemMatchFilter.apply("prefixes")

        // Assert
        val innerCriteria = nestedFilter.apply("length").criteriaObject
        val expectedDocument = Document(
            mapOf(
                "prefixes" to Document(
                    mapOf(
                        "\$elemMatch" to innerCriteria
                    )
                )
            )
        )
        assertEquals(expectedDocument, criteria.criteriaObject)
    }

}