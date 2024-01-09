package de.lise.fluxflow.springboot.cache.memory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class MemoryCacheTest {
    @Test
    fun `get should invoke the provider if object is not already cached`() {
        // Arrange
        val cachedInstance = Any()
        val provider = mock<ItemProvider<String, Any>> {
            on { get(eq("test")) } doReturn cachedInstance
        }
        val cache = MemoryCache<String, Any>()

        // Act
        cache.get("test", provider)

        // Assert
        verify(provider, times(1)).get(eq("test"))
    }

    @Test
    fun `get should the object returned by the provider if it is not already cached`() {
        // Arrange
        val cachedInstance = Any()
        val provider = mock<ItemProvider<String, Any>> {
            on { get(eq("test")) } doReturn cachedInstance
        }
        val cache = MemoryCache<String, Any>()

        // Act
        val result = cache.get("test", provider)

        // Assert
        assertThat(result).isSameAs(cachedInstance)
    }

    @Test
    fun `get should not invoke the provider if the object is already cached`() {
        // Arrange
        val cachedInstance = Any()
        val secondInstance = Any()
        val provider = mock<ItemProvider<String, Any>> {
            on { get(eq("test")) } doReturn secondInstance
        }
        val cache = MemoryCache<String, Any>()
        cache.get("test", cachedInstance)

        // Act
        cache.get("test", provider)

        // Assert
        verify(provider, never()).get(eq("test"))
    }

    @Test
    fun `get should return the cached object if it already cached`() {
        // Arrange
        val cachedInstance = Any()
        val secondInstance = Any()

        val cache = MemoryCache<String, Any>()
        cache.get("test", cachedInstance)

        // Act
        val result = cache.get("test", secondInstance)

        // Assert
        assertThat(result).isSameAs(cachedInstance)
    }
}