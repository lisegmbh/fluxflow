package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.query.filter.UnsupportedFilterException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

class MongoFilterTest {
    @Test
    fun `fromDomainFilter should produce correct mappings`() {
        // Arrange
        val expected = mapOf<Filter<*>, KClass<*>>(
            Filter.eq("Hallo") to MongoEqualFilter::class,
            Filter.property(String::length, Filter.eq(3)) to MongoPropertyFilter::class,
            Filter.or<Any>() to MongoOrFilter::class,
            Filter.and<Any>() to MongoAndFilter::class,
            Filter.endsWith("") to MongoEndsWithFilter::class,
            Filter.startsWith("") to MongoStartsWithFilter::class,
            Filter.contains("") to MongoContainsFilter::class,
            Filter.anyOf(listOf("")) to MongoAnyOfFilter::class,
            Filter.containsElement("") to MongoContainsElementFiler::class,
        )

        // Act
        val result = expected.mapValues {
            MongoFilter.fromDomainFilter(it.key)
        }

        // Assert
        result.forEach { actualMapping ->
            assertThat(actualMapping.value).isInstanceOf(
                expected[actualMapping.key]!!.java
            )
        }
    }

    @Test
    fun `fromDomainFilter should throw an exception if the filter given filter is not supported`() {
        // Arrange
        val testFilter = mock<Filter<Any>> {}

        // Act & Assert
        val thrownException = assertThrows<UnsupportedFilterException> {
            MongoFilter.fromDomainFilter(testFilter)
        }
        assertThat(thrownException.filter).isSameAs(testFilter)
    }
}