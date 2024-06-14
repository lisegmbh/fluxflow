package de.lise.fluxflow.reflection.compatibility

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.lang.reflect.Type
import java.util.function.Predicate


class RecordBuilderTest {
    @Test
    fun `typeFilter should be called to test if type should be recorded`() {
        // Arrange
        val filter = mock<Predicate<Type>> {
            on { test(any()) } doReturn false
        }
        val recordBuilder = RecordBuilder(
            filter
        )
        val testType = PersonTestModel::class.java
        
        // Act
        val record = recordBuilder.getTypeRecord(PersonTestModel::class.java)
        
        // Assert
        assertThat(record).isNull()
        verify(filter, times(1)).test(testType)
    }
    
    @Test
    fun `simple types should be recorded successfully`() {
        // Arrange
        val recordBuilder = RecordBuilder()
        
        // Act
        val record = recordBuilder.getTypeRecord(PersonTestModel::class.java)
        
        // Assert
        assertThat(record).isNotNull
    }
}