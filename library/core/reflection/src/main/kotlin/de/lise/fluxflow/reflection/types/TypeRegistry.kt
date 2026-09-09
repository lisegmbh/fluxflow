package de.lise.fluxflow.reflection.types

import java.util.Collections
import kotlin.reflect.KClass

/**
 * An immutable, role-specific mapping from persisted identifiers to trusted classes.
 */
class TypeRegistry private constructor(
    private val types: Map<TypeIdentity, TypeRegistryEntry>,
    val entries: List<TypeRegistryEntry>,
) {
    fun resolve(role: TypeRole, key: String): KClass<*> =
        types[TypeIdentity(role, key)]?.type ?: throw UnknownTypeException(role, key)

    companion object {
        fun load(
            classLoader: ClassLoader,
            additionalEntries: Iterable<TypeManifestEntry> = emptyList(),
        ): TypeRegistry = create(
            classLoader,
            TypeManifestLoader(classLoader).load() + additionalEntries,
        )

        fun create(
            classLoader: ClassLoader,
            entries: Iterable<TypeManifestEntry>,
        ): TypeRegistry {
            val inputSnapshot = entries.toList()
            inputSnapshot.forEach { entry ->
                TypeManifest.validateEntry(
                    entry.role,
                    entry.key,
                    entry.binaryClassName,
                    entry.origin,
                )
            }
            val resolved = inputSnapshot.map { entry ->
                val type = try {
                    Class.forName(entry.binaryClassName, false, classLoader).kotlin
                } catch (exception: ClassNotFoundException) {
                    throw unresolved(entry, exception)
                } catch (error: LinkageError) {
                    throw unresolved(entry, error)
                }
                ResolvedRegistration(entry, type)
            }

            val registryEntries = resolved
                .groupBy { TypeIdentity(it.entry.role, it.entry.key) }
                .map { (identity, registrations) ->
                    val byClass = registrations.groupBy { it.entry.binaryClassName }
                    if (byClass.size > 1) {
                        throw conflict(identity, registrations)
                    }
                    val registration = registrations.first()
                    TypeRegistryEntry(
                        identity.role,
                        identity.key,
                        registration.entry.binaryClassName,
                        registration.type,
                        Collections.unmodifiableList(
                            registrations.map { it.entry.origin }.distinct().sorted()
                        ),
                    )
                }
                .sortedWith(compareBy<TypeRegistryEntry> { it.role.ordinal }.thenBy { it.key })

            val entriesSnapshot = Collections.unmodifiableList(registryEntries)
            val typeSnapshot = Collections.unmodifiableMap(
                registryEntries.associateBy { TypeIdentity(it.role, it.key) }
            )
            return TypeRegistry(typeSnapshot, entriesSnapshot)
        }

        private fun unresolved(entry: TypeManifestEntry, cause: Throwable): TypeManifestException =
            TypeManifestException(
                "Could not resolve ${entry.role.manifestName} type '${entry.binaryClassName}' " +
                        "for key '${entry.key}' from '${entry.origin}'.",
                cause,
            )

        private fun conflict(
            identity: TypeIdentity,
            registrations: List<ResolvedRegistration>,
        ): TypeManifestException {
            val details = registrations
                .groupBy { it.entry.binaryClassName }
                .toSortedMap()
                .map { (className, registrationsForClass) ->
                    val origins = registrationsForClass.map { it.entry.origin }.distinct().sorted()
                    "'$className' from ${origins.joinToString(prefix = "[", postfix = "]")}"
                }
                .joinToString("; ")
            return TypeManifestException(
                "Conflicting ${identity.role.manifestName} type registrations for key " +
                        "'${identity.key}': $details."
            )
        }
    }

    private data class TypeIdentity(
        val role: TypeRole,
        val key: String,
    )

    private data class ResolvedRegistration(
        val entry: TypeManifestEntry,
        val type: KClass<*>,
    )
}

data class TypeRegistryEntry(
    val role: TypeRole,
    val key: String,
    val binaryClassName: String,
    val type: KClass<*>,
    val origins: List<String>,
)
