package de.lise.fluxflow.api.versioning

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class DefaultCompatibilityTesterTest {

    data class ExpectedVersionResult(
        val old: Version,
        val new: Version,
        val expectedResult: VersionCompatibility
    );

    @Test
    fun `checkCompatibility should return the expected results`() {
        // Arrange
        val expectedResults = listOf(
            ExpectedVersionResult(NoVersion(), NoVersion(), VersionCompatibility.Unknown),
            ExpectedVersionResult(SimpleVersion("1.0"), SimpleVersion("1.0"), VersionCompatibility.Compatible),
            ExpectedVersionResult(NoVersion(), SimpleVersion("1.0"), VersionCompatibility.Incompatible),
            ExpectedVersionResult(
                NoVersion(),
                VersionWithCompatibility(
                    SimpleVersion("1.0"),
                    listOf(NoVersion(), SimpleVersion("0.1"))
                ),
                VersionCompatibility.Compatible
            ),
            ExpectedVersionResult(SimpleVersion("1.0"), SimpleVersion("1.0"), VersionCompatibility.Compatible),
            ExpectedVersionResult(SimpleVersion("1.1"), SimpleVersion("1.2"), VersionCompatibility.Incompatible),
            ExpectedVersionResult(SimpleVersion("1.0"), NoVersion(), VersionCompatibility.Unknown)
        )
        val tester = DefaultCompatibilityTester();

        // Act && Assert
        expectedResults.forEach {
            assertThat(tester.checkCompatibility(it.old, it.new)).isEqualTo(it.expectedResult)
        }
    }
}