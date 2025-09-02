package de.lise.fluxflow.api.versioning

class DefaultCompatibilityTester(
    private val configuration: CompatibilityConfiguration = CompatibilityConfiguration()
) : CompatibilityTester {
    override fun checkCompatibility(oldVersion: Version, newVersion: Version): VersionCompatibility {
        if (oldVersion is NoVersion) {
            when (newVersion) {
                is NoVersion -> return configuration.bothUnknown
                is SimpleVersion -> return configuration.unknownToKnown
            }
        }
        if (newVersion is VersionWithCompatibility) {
            val currentResult = checkCompatibility(oldVersion, newVersion.decoratedVersion)
            if (currentResult == VersionCompatibility.Compatible) {
                return VersionCompatibility.Compatible
            }
            return newVersion.compatibleVersions.map { explicitlyCompatibleVersion ->
                if(explicitlyCompatibleVersion is NoVersion && oldVersion is NoVersion) {
                    VersionCompatibility.Compatible
                } else {
                    checkCompatibility(oldVersion, explicitlyCompatibleVersion)
                }
            }
                .plus(currentResult)
                .minOf { it }
        }
        if (newVersion is NoVersion) {
            return configuration.knownToUnknown
        }
        if (oldVersion.version == newVersion.version) {
            return VersionCompatibility.Compatible
        }
        return configuration.noExactMatch
    }
}