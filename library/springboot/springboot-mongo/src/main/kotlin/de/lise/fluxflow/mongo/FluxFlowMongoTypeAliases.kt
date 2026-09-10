package de.lise.fluxflow.mongo

import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException

/** Immutable aliases accepted by the internal FluxFlow Mongo converter. */
internal class FluxFlowMongoTypeAliases(
    registry: TypeRegistry,
    val rootTypes: Set<Class<*>>,
    val infrastructureTypes: Set<Class<*>>,
) {
    private data class Registration(
        val alias: String,
        val type: Class<*>,
        val role: TypeRole?,
    )

    private val aliases: Map<String, Class<*>>
    private val roleAliases: Map<Pair<TypeRole, String>, Class<*>>
    private val writeTypes: Set<Class<*>>
    private val infrastructureAliases = infrastructureTypes.mapTo(mutableSetOf(), Class<*>::getName)

    init {
        val applicationRegistrations = registry.entries
            .filter { it.role == TypeRole.MODEL || it.role == TypeRole.VALUE }
            .flatMap { entry ->
                setOf(entry.key, entry.binaryClassName).map { alias ->
                    Registration(alias, entry.type.java, entry.role)
                }
            }
        val fixedRegistrations = (rootTypes + infrastructureTypes).map { type ->
            Registration(type.name, type, null)
        }
        val registrations = applicationRegistrations + fixedRegistrations

        aliases = registrations
            .groupBy(Registration::alias)
            .mapValues { (alias, matches) ->
                val types = matches.map(Registration::type).distinct()
                if (types.size != 1) {
                    throw TypeManifestException(
                        "Mongo type alias '$alias' resolves to multiple classes: " +
                                types.joinToString { it.name }
                    )
                }
                types.single()
            }
        roleAliases = applicationRegistrations
            .groupBy { requireNotNull(it.role) to it.alias }
            .mapValues { (identity, matches) ->
                val types = matches.map(Registration::type).distinct()
                if (types.size != 1) {
                    throw TypeManifestException(
                        "Mongo ${identity.first.name.lowercase()} type alias '${identity.second}' " +
                                "resolves to multiple classes: ${types.joinToString { it.name }}"
                    )
                }
                types.single()
            }
        writeTypes = registrations.map(Registration::type).toSet()
    }

    fun resolve(alias: Any?): Class<*> {
        require(alias is String) { "Mongo type alias must be a string" }
        require(alias.isNotEmpty()) { "Mongo type alias must not be empty" }
        return aliases[alias]
            ?: throw IllegalArgumentException("Unregistered Mongo type alias '$alias'")
    }

    fun resolve(role: TypeRole, alias: Any?): Class<*> {
        require(alias is String) { "Mongo type alias must be a string" }
        require(alias.isNotEmpty()) { "Mongo type alias must not be empty" }
        return roleAliases[role to alias] ?: throw UnknownTypeException(role, alias)
    }

    fun aliasFor(type: Class<*>): String {
        if (type !in writeTypes) {
            throw IllegalArgumentException("Unregistered Mongo type '${type.name}' cannot be persisted")
        }
        // FluxFlow historically persists fully-qualified class names. Keep that stable while
        // accepting both FQCNs and explicit manifest keys on reads.
        return type.name
    }

    fun isInfrastructure(alias: Any?): Boolean = alias is String && alias in infrastructureAliases
}
