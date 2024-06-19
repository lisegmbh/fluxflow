package de.lise.fluxflow.api.versioning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SimpleVersionTest {
    @Test
    fun `simple versions should be considered equal, if they match exactly`() {
        // Arrange
        val simpleVersion = SimpleVersion("test")
        val otherVersion = mock<Version> {
            on { version } doReturn "test"
        }

        // Act
        val result = simpleVersion.checkCompatibilityTo(otherVersion)

        // Assert
        assertThat(result).isEqualTo(VersionCompatibility.Compatible)
        verify(otherVersion, times(1)).version
    }

    @Test
    fun `simple versions should return the provided mismatchCompatibility if the do not match`() {
        // Arrange
        val simpleVersionReturningIncompatible = SimpleVersion("test", VersionCompatibility.Incompatible)
        val simpleVersionReturningUnknown = SimpleVersion("test", VersionCompatibility.Unknown)
        val otherVersion = mock<Version> {
            on { version } doReturn "something other than test"
        }

        // Act
        val incompatibleResult = simpleVersionReturningIncompatible.checkCompatibilityTo(otherVersion)
        val unknownResult = simpleVersionReturningUnknown.checkCompatibilityTo(otherVersion)

        // Assert
        assertThat(incompatibleResult).isEqualTo(VersionCompatibility.Incompatible)
        assertThat(unknownResult).isEqualTo(unknownResult)
    }

    @Test
    fun `simple versions should use Unknown as the default mismatch compatibility`() {
        // Arrange
        val simpleVersion = SimpleVersion("test")
        val otherVersion = mock<Version> {
            on { version } doReturn "something other than test"
        }

        // Act
        val result = simpleVersion.checkCompatibilityTo(otherVersion)

        // Assert
        assertThat(result).isEqualTo(VersionCompatibility.Unknown)
    }
}