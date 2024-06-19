package de.lise.fluxflow.api.versioning

/**
 * Represents a version, that is used to determine compatibility among different revisions of various workflow elements.
 */
interface Version {
    /**
     * This version's string representation.
     */
    val version: String

    /**
     * Checks if this version is compatible to the [other] version.
     */
    fun checkCompatibilityTo(other: Version): VersionCompatibility

    companion object {
        @JvmStatic
        fun parse(version: String?): Version {
            if(version == null || version == "") {
                return NoVersion()
            }
            return SimpleVersion(version)
        }
    }
}