package de.lise.fluxflow.api.versioning

/**
 * This is the most basic [Version] implementation, that only compares the string representations returned by
 * [version].
 *
 * 1. If they match, [VersionCompatibility.Compatible] will be returned
 * 2. If the other version is [NoVersion], [VersionCompatibility.Unknown] is returned.
 * 3. If they don't match, [mismatchCompatibility]
 * (which is set to [VersionCompatibility.Incompatible] by default) will be returned.
 *
 * @param version This version's string representation.
 * @param mismatchCompatibility The compatibility result that should be returned when checking the compatibility with
 * a differing version (default: [VersionCompatibility.Incompatible]).
 */
class SimpleVersion(
    override val version: String,
    private val mismatchCompatibility: VersionCompatibility = VersionCompatibility.Incompatible
) : Version