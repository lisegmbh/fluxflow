package de.lise.fluxflow.mongo.query.sort

import de.lise.fluxflow.query.sort.Sort
import de.lise.fluxflow.query.sort.UnsupportedSortException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

class MongoSortTest {
    @Test
    fun `toMongoSort should return the correctly mapped mongo sort operation`() {
        // Arrange
        val expectedMappings = mapOf<Sort<*>, KClass<*>>(
            Sort.asc<String>() to MongoDefaultSort::class,
            Sort.desc<String>() to MongoDefaultSort::class,
            Sort.property(String::length, Sort.asc()) to MongoPropertySort::class,
        )

        // Act
        val mappings = expectedMappings.mapValues {
            MongoSort.toMongoSort(it.key)
        }

        // Assert
        mappings.forEach { mapping ->
            assertThat(mapping.value).isInstanceOf(expectedMappings[mapping.key]!!.java)
        }
    }

    @Test
    fun `toMongoSort should throw an exception if an unsupported sort is provided`() {
        // Arrange
        val unknownSort = mock<Sort<String>> {}

        // Act & Assert
        val exception = assertThrows<UnsupportedSortException> {
            MongoSort.toMongoSort(unknownSort)
        }
        assertThat(exception.sort).isSameAs(unknownSort)
    }
}