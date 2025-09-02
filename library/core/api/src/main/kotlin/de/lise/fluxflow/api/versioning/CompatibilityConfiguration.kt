package de.lise.fluxflow.api.versioning

data class CompatibilityConfiguration(
    var bothUnknown: VersionCompatibility = VersionCompatibility.Unknown,
    var unknownToKnown: VersionCompatibility = VersionCompatibility.Incompatible,
    var knownToUnknown: VersionCompatibility = VersionCompatibility.Unknown,
    var noExactMatch: VersionCompatibility = VersionCompatibility.Incompatible
)