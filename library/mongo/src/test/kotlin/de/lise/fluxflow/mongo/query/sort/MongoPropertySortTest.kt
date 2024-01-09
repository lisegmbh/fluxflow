package de.lise.fluxflow.mongo.query.sort

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.data.mongodb.core.mapping.Field

class MongoPropertySortTest {
    @Test
    fun `apply should invoke the nested sort using the prefixed path`() {
        // Arrange
        val nestedSort = mock<MongoSort<Int>> {}
        val mongoPropertySort = MongoPropertySort(
            String::length,
            nestedSort
        )
        
        // Act
        mongoPropertySort.apply("prefix")
        
        // Assert
        verify(nestedSort, times(1)).apply("prefix.length")
    }
    
    data class TestDocument(@Field("overwrittenName") val property: String)
    @Test
    fun `apply should use the mongo field name from @Field annotation`() {
        // Arrange
        val nestedSort = mock<MongoSort<String>> {}
        val mongoPropertySort = MongoPropertySort(
            TestDocument::property,
            nestedSort
        )
        
        // Act
        mongoPropertySort.apply("prefix")
        
        // Assert
        verify(nestedSort, times(1)).apply("prefix.overwrittenName")
    }
    
}