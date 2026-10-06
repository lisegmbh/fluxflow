package de.lise.fluxflow.reflection.types

/** The parsed contents of one FluxFlow type manifest resource. */
data class TypeManifestContents(
    val entries: List<TypeManifestEntry>,
    val coveredPackages: List<String> = emptyList(),
)
