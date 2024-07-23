package de.lise.fluxflow.api.versioning

/**
 * Represents a version, that is used to determine compatibility among different revisions of various workflow elements.
 */
interface Version {
    /**
     * This version's string representation.
     */
    val version: String

    companion object {
        @JvmStatic
        fun parse(version: String?): Version {
            if(version.isNullOrBlank()) {
                return NoVersion()
            }
            return SimpleVersion(version)
        }
    }
}