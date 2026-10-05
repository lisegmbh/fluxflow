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
        ): TypeRegistry = load(classLoader, additionalEntries, emptyList())

        fun load(
            classLoader: ClassLoader,
            additionalEntries: Iterable<TypeManifestEntry>,
            resolvedEntries: Iterable<TypeRegistryEntry>,
        ): TypeRegistry = create(
            classLoader,
            TypeManifestLoader(classLoader).load() + additionalEntries,
            resolvedEntries,
        )

        fun create(
            classLoader: ClassLoader,
            entries: Iterable<TypeManifestEntry>,
        ): TypeRegistry = create(classLoader, entries, emptyList())

        fun create(
            classLoader: ClassLoader,
            entries: Iterable<TypeManifestEntry>,
            resolvedEntries: Iterable<TypeRegistryEntry>,
        ): TypeRegistry {
            val explicit = resolvedEntries.flatMap { registration ->
                if (registration.binaryClassName != registration.type.java.name) {
                    throw TypeManifestException("A resolved registration must use its class's binary name.")
                }
                if (registration.origins.isEmpty()) {
                    throw TypeManifestException("A resolved registration must declare its origin.")
                }
                registration.origins.map { origin ->
                    ResolvedRegistration(
                        TypeManifestEntry(registration.role, registration.key, registration.binaryClassName, origin),
                        registration.type,
                    )
                }
            }
            val inputSnapshot = entries.sortedWith(TypeManifest.entryOrder)
            TypeManifest.normalize(inputSnapshot + explicit.map { it.entry })
            val resolved = (inputSnapshot.map { entry ->
                val type = try {
                    Class.forName(entry.binaryClassName, false, classLoader).kotlin
                } catch (exception: ClassNotFoundException) {
                    throw unresolved(entry, exception)
                } catch (error: LinkageError) {
                    throw unresolved(entry, error)
                }
                ResolvedRegistration(entry, type)
            } + explicit).sortedWith { first, second -> TypeManifest.entryOrder.compare(first.entry, second.entry) }

            resolved.groupBy { TypeIdentity(it.entry.role, it.entry.key) }
                .entries.firstOrNull { (_, registrations) -> registrations.map { it.type.java }.distinct().size > 1 }
                ?.let { (identity, registrations) ->
                    throw TypeRegistrationConflictException(
                        identity.role,
                        identity.key,
                        "Conflicting ${identity.role.manifestName} type registrations for key '${identity.key}': " +
                            "distinct classes from different class loaders share binary name " +
                            "'${registrations.first().entry.binaryClassName}' from " +
                            registrations.map { it.entry.origin }.distinct().sorted().joinToString() + "."
                    )
                }

            val registryEntries = resolved
                .groupBy { TypeIdentity(it.entry.role, it.entry.key) }
                .map { (identity, registrations) ->
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
