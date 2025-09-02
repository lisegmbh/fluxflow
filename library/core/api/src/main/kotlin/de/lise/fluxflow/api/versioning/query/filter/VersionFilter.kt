package de.lise.fluxflow.api.versioning.query.filter

import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.query.filter.Filter

data class VersionFilter(
    val version: Filter<String>
) {
    companion object {
        @JvmStatic
        fun eq(version: Version): VersionFilter {
            return VersionFilter(
                Filter.eq(version.version)
            )
        }
        
        @JvmStatic
        fun anyOf(versions: Collection<Version>): VersionFilter {
            return VersionFilter(
                Filter.anyOf(
                    versions.map { it.version }
                )
            )
        }
        
        @JvmStatic
        fun anyOf(vararg version: Version): VersionFilter {
            return anyOf(version.toList())
        }
    }
}
