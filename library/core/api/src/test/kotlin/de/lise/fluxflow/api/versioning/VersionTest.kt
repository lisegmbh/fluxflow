package de.lise.fluxflow.api.versioning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VersionTest {
    @Test
    fun `parse should return a NoVersion if the given value is null`() {
        // Act
        val result = Version.parse(null)

        // Assert
        assertThat(result).isInstanceOf(NoVersion::class.java)
    }

    @Test
    fun `parse should return a NoVersion if the given value is an empty string`() {
        // Act
        val result = Version.parse("")

        // Assert
        assertThat(result).isInstanceOf(NoVersion::class.java)
    }


    @Test
    fun `parse should return a NoVersion if the given value is a blank string`() {
        // Act
        val result = Version.parse("  ")

        // Assert
        assertThat(result).isInstanceOf(NoVersion::class.java)
    }

    @Test
    fun `parse should otherwise return a SimpleVersion`() {
        // Arrange
        val versionString = "any other value"

        // Act
        val result = Version.parse(versionString)

        // Assert
        assertThat(result).isInstanceOf(SimpleVersion::class.java)
        assertThat(result.version).isEqualTo(versionString)
    }
}