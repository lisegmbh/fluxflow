package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeRole
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object ManifestDeclarationCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(role: TypeRole, key: String, binaryClassName: String): String =
        listOf(role.name, encodePart(key), encodePart(binaryClassName)).joinToString(";")

    fun decode(value: String): ManifestDeclaration {
        val parts = value.split(';')
        require(parts.size == 3) { "Invalid encoded FluxFlow type declaration." }
        return ManifestDeclaration(
            TypeRole.valueOf(parts[0]),
            decodePart(parts[1]),
            decodePart(parts[2]),
        )
    }

    private fun encodePart(value: String): String = encoder.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8)
    )

    private fun decodePart(value: String): String = String(
        decoder.decode(value),
        StandardCharsets.UTF_8,
    )
}

internal data class ManifestDeclaration(
    val role: TypeRole,
    val key: String,
    val binaryClassName: String,
)
