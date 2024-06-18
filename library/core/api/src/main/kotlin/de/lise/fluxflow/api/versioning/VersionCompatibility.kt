package de.lise.fluxflow.api.versioning

enum class VersionCompatibility {
    // Be aware that the enum element order also determines the sorting order.
    Compatible,
    Unknown,
    Incompatible
}