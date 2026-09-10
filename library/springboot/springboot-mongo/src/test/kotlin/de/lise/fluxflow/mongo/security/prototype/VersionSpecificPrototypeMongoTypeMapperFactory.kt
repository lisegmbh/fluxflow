package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.data.convert.TypeInformationMapper
import org.springframework.data.mapping.Alias
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MongoTypeMapper
import org.springframework.data.util.TypeInformation

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
        private val aliases = PrototypeMongoAliases(registry, trustedRootTypes)

        override fun resolveTypeFrom(alias: Alias): TypeInformation<*>? {
            if (!alias.isPresent) {
                return null
            }
            return TypeInformation.of(aliases.resolve(alias.value))
        }

        override fun createAliasFor(type: TypeInformation<*>): Alias =
            Alias.of(aliases.aliasFor(type.type))
    }
}
