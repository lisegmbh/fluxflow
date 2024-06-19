package de.lise.fluxflow.api.versioning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class VersionWithCompatibilityTest {
    @Test
    fun `checkCompatibilityTo should return Compatible if the decorated versions is compatible`() {
        // Arrange
        val otherVersion = mock<Version> {}

        val decoratedVersion = mockVersion(VersionCompatibility.Compatible)
        val version = VersionWithCompatibility(decoratedVersion, emptyList())


        // Act
        val result = version.checkCompatibilityTo(otherVersion)

        // Assert
        assertThat(result).isEqualTo(VersionCompatibility.Compatible)
        verify(decoratedVersion, times(1)).checkCompatibilityTo(otherVersion)
    }

    @Test
    fun `checkCompatibilityTo should return the best result of any known compatible versions`() {
        // Arrange
        val otherVersion = mock<Version> {}

        val decoratedVersion = mockVersion(VersionCompatibility.Incompatible)
        val versionWithUnknownBeingTheBest = VersionWithCompatibility(
            decoratedVersion,
            listOf(
                mockVersion(VersionCompatibility.Incompatible),
                mockVersion(VersionCompatibility.Unknown),
            )
        )
        val versionWithCompatibleBeingTheBest = VersionWithCompatibility(
            decoratedVersion,
            listOf(
                mockVersion(VersionCompatibility.Incompatible),
                mockVersion(VersionCompatibility.Unknown),
                mockVersion(VersionCompatibility.Compatible)
            )
        )


        // Act
        val compatibleResult = versionWithCompatibleBeingTheBest.checkCompatibilityTo(otherVersion)
        val unknownResult = versionWithUnknownBeingTheBest.checkCompatibilityTo(otherVersion)

        // Assert
        assertThat(compatibleResult).isEqualTo(VersionCompatibility.Compatible)
        assertThat(unknownResult).isEqualTo(VersionCompatibility.Unknown)
    }

    @Test
    fun `checkCompatibilityTo should return Incompatible if there is no compatible version`() {
        // Arrange
        val otherVersion = mock<Version> {}

        val decoratedVersion = mockVersion(VersionCompatibility.Incompatible)
        val knownCompatibleVersions = listOf(
            mockVersion(VersionCompatibility.Incompatible),
        )
        val version = VersionWithCompatibility(decoratedVersion, knownCompatibleVersions)

        // Act
        val result = version.checkCompatibilityTo(otherVersion)

        // Assert
        assertThat(result).isEqualTo(VersionCompatibility.Incompatible)
    }

    private fun mockVersion(compatibility: VersionCompatibility): Version {
        return mock<Version> {
            on { checkCompatibilityTo(any()) } doReturn compatibility
        }
    }
}