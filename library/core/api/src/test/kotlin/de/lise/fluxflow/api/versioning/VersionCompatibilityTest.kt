package de.lise.fluxflow.api.versioning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VersionCompatibilityTest {
    @Test
    fun `version compatibilities should get sorted from most to least favorable`() {
        // Arrange
        val randomOrder = listOf(
            VersionCompatibility.Unknown,
            VersionCompatibility.Incompatible,
            VersionCompatibility.Compatible
        )

        // Act
        val sortedOrder = randomOrder.sorted()

        // Assert
        assertThat(sortedOrder).containsExactly(
            VersionCompatibility.Compatible,
            VersionCompatibility.Unknown,
            VersionCompatibility.Incompatible
        )
    }
}