package de.lise.fluxflow.stereotyped.versioning

import de.lise.fluxflow.api.versioning.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VersionBuilderTest {
    @Test
    fun `build should return NoVersion if there is no version`() {
        // Arrange
        val builder = VersionBuilder()

        // Act
        val result = builder.build(StepWithoutAVersion::class)

        // Assert
        assertThat(result).isInstanceOf(NoVersion::class.java)
    }

    @Test
    fun `build should return a simple version if there are no known compatible versions`() {
        // Arrange
        val builder = VersionBuilder()

        // Act
        val result = builder.build(StepWithSimpleVersion::class)

        // Assert
        assertThat(result).isInstanceOf(SimpleVersion::class.java)
        assertThat(result.version).isEqualTo("0.0.1")
    }

    @Test
    fun `build should return a version with compatible versions if they are given`() {
        // Arrange
        val builder = VersionBuilder()
        val compatibilityTester = DefaultCompatibilityTester()

        // Act
        val result = builder.build(StepWithCompatibleVersions::class)

        // Assert
        assertThat(result).isInstanceOf(VersionWithCompatibility::class.java)
        assertThat(compatibilityTester.checkCompatibility(SimpleVersion("0.0.1"), result)).isEqualTo(VersionCompatibility.Compatible)
        assertThat(compatibilityTester.checkCompatibility(SimpleVersion("0.0.2"), result)).isEqualTo(VersionCompatibility.Compatible)
        assertThat(compatibilityTester.checkCompatibility(SimpleVersion("0.0.3"), result)).isEqualTo(VersionCompatibility.Compatible)
    }

    private class StepWithoutAVersion
    @Version("0.0.1")
    private class StepWithSimpleVersion
    @Version("0.0.3", compatibleVersions = ["0.0.1", "0.0.2"])
    private class StepWithCompatibleVersions
}