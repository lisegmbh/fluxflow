package de.lise.fluxflow.api.versioning

data class CompatibilityConfiguration(
    val bothUnknown: VersionCompatibility = VersionCompatibility.Unknown,
    val unknownToKnown: VersionCompatibility = VersionCompatibility.Incompatible,
    val knownToUnknown: VersionCompatibility = VersionCompatibility.Unknown,
    val noExactMatch: VersionCompatibility = VersionCompatibility.Incompatible
)