package de.lise.fluxflow.api.versioning

/**
 * The [NoVersion] indicates missing versioning information.
 */
class NoVersion : Version {
    /**
     * Always returns an empty string.
     */
    override val version: String
        get() = ""

    /**
     * Always returns [VersionCompatibility.Unknown].
     */
    override fun checkCompatibilityTo(other: Version): VersionCompatibility {
        return VersionCompatibility.Unknown
    }
}