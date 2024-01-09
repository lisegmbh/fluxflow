package de.lise.fluxflow.mongo.query.filter

import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.data.mongodb.core.mapping.Field

class MongoPropertyFilterTest {
    @Test
    fun `property filter should invoke the nested value filter using the concatenated path`() {
        // Arrange
        val nestedValueFilter = mock<MongoFilter<Int>> {}
        val mongoPropertyFilter = MongoPropertyFilter(
            String::length,
            nestedValueFilter
        )
        
        // Act
        mongoPropertyFilter.apply("prefix")
        
        // Assert
        verify(nestedValueFilter, times(1)).apply(eq("prefix.length"))
    }
    
    data class TestDocument(
        @Field(value = "renamedProperty")
        val property: String
    )
    @Test
    fun `property filter should consider field names overwritten by annotation`() {
        // Arrange
        val nestedValueFilter = mock<MongoFilter<String>> {}
        val mongoPropertyFilter = MongoPropertyFilter(
            TestDocument::property,
            nestedValueFilter
        )

        // Act
        mongoPropertyFilter.apply("prefix")

        // Assert
        verify(nestedValueFilter, times(1)).apply(eq("prefix.renamedProperty"))
    }
}