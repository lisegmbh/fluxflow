package de.lise.fluxflow.api.versioning

/**
 * This is the most basic [Version] implementation, that only compares the string representations returned by
 * [version].
 * 1. If they match, [VersionCompatibility.Compatible] will be returned
 * 2. If they don't match, [mismatchCompatibility] will be returned.
 */
class SimpleVersion(
  override val version: String,
  private val mismatchCompatibility: VersionCompatibility = VersionCompatibility.Unknown
): Version {
    override fun checkCompatibilityTo(other: Version): VersionCompatibility {
        if(version == other.version) {
            return VersionCompatibility.Compatible
        }
        return mismatchCompatibility
    }
}