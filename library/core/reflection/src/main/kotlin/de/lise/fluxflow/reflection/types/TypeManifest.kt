package de.lise.fluxflow.reflection.types

import java.io.Reader
import java.util.Properties

/**
 * Reads and writes the versioned type inventory contained in FluxFlow artifacts.
 */
object TypeManifest {
    const val RESOURCE_PATH = "META-INF/fluxflow/type-manifest.properties"
    const val VERSION = "1"

    private const val VERSION_PROPERTY = "manifest.version"
    private const val COVERED_PACKAGES_PROPERTY = "manifest.covered-packages"

    val entryOrder: Comparator<TypeManifestEntry> = compareBy<TypeManifestEntry> { it.role.ordinal }
        .thenBy { it.key }
        .thenBy { it.binaryClassName }
        .thenBy { it.origin }

    fun read(origin: String, source: Reader): TypeManifestContents {
        val entries = mutableListOf<TypeManifestEntry>()
        val propertyLines = mutableMapOf<String, Int>()
        var version: String? = null
        var coveredPackages = emptyList<String>()

        source.buffered().use { reader ->
            reader.lineSequence().forEachIndexed { index, line ->
                val lineNumber = index + 1
                val trimmed = line.trimStart()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    return@forEachIndexed
                }
                if (hasContinuation(line)) {
                    invalid(origin, lineNumber, "continued properties are not supported")
                }

                val property = try {
                    Properties().apply { load(line.reader()) }
                } catch (exception: IllegalArgumentException) {
                    throw TypeManifestException(
                        "Invalid FluxFlow type manifest '$origin' at line $lineNumber: ${exception.message}",
                        exception,
                    )
                }
                if (property.size != 1) {
                    invalid(origin, lineNumber, "expected one property")
                }

                val name = property.stringPropertyNames().single()
                val previousLine = propertyLines.putIfAbsent(name, lineNumber)
                if (previousLine != null) {
                    invalid(
                        origin,
                        lineNumber,
                        "duplicate property '$name' (first declared at line $previousLine)",
                    )
                }
                val value = property.getProperty(name)

                if (name == VERSION_PROPERTY) {
                    version = value
                    return@forEachIndexed
                }
                if (name == COVERED_PACKAGES_PROPERTY) {
                    coveredPackages = parseCoveredPackages(value, origin, lineNumber)
                    return@forEachIndexed
                }

                val separator = name.indexOf('.')
                val roleName = name.take(separator.coerceAtLeast(0))
                val role = TypeRole.fromManifestName(roleName)
                    ?: invalid(origin, lineNumber, "unknown property '$name'")
                val key = if (separator >= 0) name.substring(separator + 1) else ""
                validateEntry(role, key, value, "$origin at line $lineNumber")
                entries += TypeManifestEntry(role, key, value, "$origin at line $lineNumber")
            }
        }

        if (version == null) {
            throw TypeManifestException(
                "Invalid FluxFlow type manifest '$origin': missing property '$VERSION_PROPERTY'."
            )
        }
        if (version != VERSION) {
            throw TypeManifestException(
                "Invalid FluxFlow type manifest '$origin': unsupported version '$version'."
            )
        }
        return TypeManifestContents(entries, coveredPackages)
    }

    fun write(
        entries: Iterable<TypeManifestEntry>,
        coveredPackages: Iterable<String> = emptyList(),
    ): String {
        val normalized = normalize(entries).distinctBy { Triple(it.role, it.key, it.binaryClassName) }
        val packages = normalizeCoveredPackages(coveredPackages)

        return buildString {
            append(VERSION_PROPERTY).append('=').append(VERSION).append('\n')
            if (packages.isNotEmpty()) {
                append(COVERED_PACKAGES_PROPERTY).append('=').append(packages.joinToString(",")).append('\n')
            }
            normalized.forEach { entry ->
                append(entry.role.manifestName)
                    .append('.')
                    .append(escapeKey(entry.key))
                    .append('=')
                    .append(escapeValue(entry.binaryClassName))
                    .append('\n')
            }
        }
    }

    /** Validates and orders declarations while retaining every registration's origin. */
    fun normalize(entries: Iterable<TypeManifestEntry>): List<TypeManifestEntry> {
        val normalized = entries.sortedWith(entryOrder)
        normalized.forEach { validateEntry(it.role, it.key, it.binaryClassName, it.origin) }
        normalized.groupBy { it.role to it.key }.entries
            .firstOrNull { (_, registrations) -> registrations.map { it.binaryClassName }.distinct().size > 1 }
            ?.let { (identity, registrations) ->
                val details = registrations.groupBy { it.binaryClassName }.toSortedMap()
                    .map { (className, registrationsForClass) ->
                        val origins = registrationsForClass.map { it.origin }.distinct().sorted()
                        "'$className' from ${origins.joinToString(prefix = "[", postfix = "]")}"
                    }.joinToString("; ")
                throw TypeRegistrationConflictException(identity.first, identity.second,
                    "Conflicting ${identity.first.manifestName} type registrations for key '${identity.second}': $details.")
            }
        return normalized
    }

    fun covers(packageName: String, coveredPackages: Iterable<String>): Boolean =
        coveredPackages.any { packageName == it || packageName.startsWith("$it.") }

    internal fun validateEntry(
        role: TypeRole,
        key: String,
        binaryClassName: String,
        origin: String,
    ) {
        if (key.isBlank()) {
            throw TypeManifestException(
                "Invalid FluxFlow type registration from '$origin': ${role.manifestName} key must not be blank."
            )
        }
        if (binaryClassName.isBlank() || binaryClassName.any(Char::isWhitespace)) {
            throw TypeManifestException(
                "Invalid FluxFlow type registration from '$origin': class name '$binaryClassName' is invalid."
            )
        }
        if (origin.isBlank()) {
            throw TypeManifestException("A FluxFlow type registration must declare its origin.")
        }
    }

    private fun parseCoveredPackages(value: String, origin: String, lineNumber: Int): List<String> {
        val packages = value.split(',').map(String::trim).filter(String::isNotEmpty)
        if (packages.any { it.any(Char::isWhitespace) || it.startsWith('.') || it.endsWith('.') }) {
            invalid(origin, lineNumber, "invalid covered package list '$value'")
        }
        return normalizeCoveredPackages(packages)
    }

    private fun normalizeCoveredPackages(packages: Iterable<String>): List<String> =
        packages.map(String::trim).filter(String::isNotEmpty).distinct().sorted()

    private fun hasContinuation(line: String): Boolean {
        var backslashes = 0
        for (character in line.reversed()) {
            if (character != '\\') {
                break
            }
            backslashes++
        }
        return backslashes % 2 == 1
    }

    private fun escapeKey(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u000c' -> append("\\f")
                ' ', '=', ':', '#', '!' -> append('\\').append(character)
                else -> append(character)
            }
        }
    }

    private fun escapeValue(value: String): String = buildString {
        value.forEachIndexed { index, character ->
            when (character) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u000c' -> append("\\f")
                ' ' -> if (index == 0) append("\\ ") else append(character)
                else -> append(character)
            }
        }
    }

    private fun invalid(origin: String, lineNumber: Int, message: String): Nothing {
        throw TypeManifestException(
            "Invalid FluxFlow type manifest '$origin' at line $lineNumber: $message."
        )
    }
}
