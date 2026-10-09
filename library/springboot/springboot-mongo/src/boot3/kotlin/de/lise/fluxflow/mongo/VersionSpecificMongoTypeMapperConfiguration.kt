package de.lise.fluxflow.mongo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.convert.TypeInformationMapper
import org.springframework.data.mapping.Alias
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MongoTypeMapper
import org.springframework.data.util.TypeInformation

@Configuration(proxyBeanMethods = false)
internal open class VersionSpecificMongoTypeMapperConfiguration {
    @Bean
    internal open fun fluxFlowMongoTypeMapperFactory(): FluxFlowMongoTypeMapperFactory =
        VersionSpecificMongoTypeMapperFactory()
}

private class VersionSpecificMongoTypeMapperFactory : FluxFlowMongoTypeMapperFactory {
    override fun create(aliases: FluxFlowMongoTypeAliases, typeKey: String): MongoTypeMapper =
        DefaultMongoTypeMapper(typeKey, listOf(RegistryTypeInformationMapper(aliases)))

    private class RegistryTypeInformationMapper(
        private val aliases: FluxFlowMongoTypeAliases,
    ) : TypeInformationMapper {
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
