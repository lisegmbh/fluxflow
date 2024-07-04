package de.lise.fluxflow.api.versioning

/**
 * This version implementation is a decorator for other [Version]s, that can be used to
 * add a static listing of versions that are known to be compatible.
 */
class VersionWithCompatibility(
    private val decoratedVersion: Version,
    val compatibleVersions: List<Version>
) : Version {
    override val version: String
        get() = decoratedVersion.version

    override fun checkCompatibilityTo(other: Version): VersionCompatibility {
        val currentResult = decoratedVersion.checkCompatibilityTo(other)
        if (currentResult == VersionCompatibility.Compatible) {
            return VersionCompatibility.Compatible
        }

        return compatibleVersions.map {
            it.checkCompatibilityTo(other)
        }.plus(currentResult).minOf { it }
    }
}