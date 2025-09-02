package de.lise.fluxflow.api.versioning

enum class VersionCompatibility : Comparable<VersionCompatibility> {
    // Be aware that the enum element order also determines the sorting order.
    Compatible,
    Unknown,
    Incompatible;

    /**
     * Checks if the given compatibility is better than or equal to this one.
     * When comparing compatibilities, the following logic applies:
     *
     * - The same compatibility is always satisfying
     * - [Incompatible] is always satisfied by any other compatibility
     * - [Unknown] is will be satisfied by itself and [Compatible]
     * - [Compatible] will only be satisfied when compared to itself
     *
     * @param other The compatibility which must be better or equal to this one.
     * @return `true`, if the [other] compatibility is better than or equal to this one.
     */
    fun isSatisfiedBy(other: VersionCompatibility): Boolean {
        return when(this) {
            Incompatible -> true
            Unknown -> other == Unknown || other == Compatible
            Compatible -> other == Compatible
        }
    }
}