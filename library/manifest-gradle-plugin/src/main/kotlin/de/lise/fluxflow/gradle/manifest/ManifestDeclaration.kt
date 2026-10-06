package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeRole
import java.io.Serializable

data class ManifestDeclaration(
    val role: TypeRole,
    val key: String,
    val binaryClassName: String,
) : Serializable
