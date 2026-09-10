package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRole
import org.springframework.data.convert.TypeInformationMapper
import org.springframework.data.core.TypeInformation
import org.springframework.data.mapping.Alias
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MongoTypeMapper

class VersionSpecificPrototypeMongoTypeMapperFactory : PrototypeMongoTypeMapperFactory {
    override fun create(
        registry: TypeRegistry,
        typeKey: String,
        trustedRootTypes: Set<Class<*>>,
    ): MongoTypeMapper = DefaultMongoTypeMapper(
        typeKey,
        listOf(RegistryTypeInformationMapper(registry, trustedRootTypes)),
    )

    private class RegistryTypeInformationMapper(
        registry: TypeRegistry,
        trustedRootTypes: Set<Class<*>>,
    ) : TypeInformationMapper {
        private val aliases: Map<String, Class<*>>
        private val writeAliases: Map<Class<*>, String>

        init {
            val registrations = registry.entries
                .filter { it.role == TypeRole.MODEL || it.role == TypeRole.VALUE }
                .map { Registration(it.key, it.type.java) } +
                    trustedRootTypes.map { Registration(it.name, it) }
            aliases = readAliases(registrations)
            writeAliases = writeAliases(registrations)
        }

        override fun resolveTypeFrom(alias: Alias): TypeInformation<*>? {
            if (!alias.isPresent) {
                return null
            }
            val value = alias.value
            require(value is String) { "Mongo type alias must be a string" }
            require(value.isNotEmpty()) { "Mongo type alias must not be empty" }
            val type = aliases[value]
                ?: throw IllegalArgumentException("Unregistered Mongo type alias '$value'")
            return TypeInformation.of(type)
        }

        override fun createAliasFor(type: TypeInformation<*>): Alias {
            val rawType = type.type
            val alias = writeAliases[rawType]
                ?: throw IllegalArgumentException(
                    "Unregistered Mongo type '${rawType.name}' cannot be persisted"
                )
            return Alias.of(alias)
        }

        private data class Registration(val alias: String, val type: Class<*>)

        private companion object {
            fun readAliases(registrations: List<Registration>): Map<String, Class<*>> =
                registrations.groupBy { it.alias }.mapValues { (alias, matches) ->
                    val types = matches.map { it.type }.distinct()
                    if (types.size != 1) {
                        throw TypeManifestException(
                            "Mongo type alias '$alias' resolves to multiple classes"
                        )
                    }
                    types.single()
                }

            fun writeAliases(registrations: List<Registration>): Map<Class<*>, String> =
                registrations.groupBy { it.type }.mapValues { (type, matches) ->
                    val aliases = matches.map { it.alias }.distinct()
                    aliases.singleOrNull { it == type.name }
                        ?: aliases.singleOrNull()
                        ?: throw TypeManifestException(
                            "Mongo type '${type.name}' has no unique write alias"
                        )
                }
        }
    }
}
