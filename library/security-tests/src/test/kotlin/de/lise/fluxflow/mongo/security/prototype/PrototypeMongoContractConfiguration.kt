package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@TestConfiguration
open class PrototypeMongoContractConfiguration {
    @Bean
    open fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions.create { adapter ->
            adapter.registerConverter(PrototypeConvertedValueWriter)
            adapter.registerConverter(PrototypeConvertedValueReader)
        }

    @Bean("prototypeTypeRegistry")
    open fun prototypeTypeRegistry(): TypeRegistry =
        prototypeRegistry(WitnessClassLoader(), *allowedPrototypeEntries())

    @Bean
    @Primary
    open fun hostTypeRegistry(): TypeRegistry = prototypeRegistry(
        WitnessClassLoader(),
        *allowedPrototypeEntries(),
        prototypeEntry(
            TypeRole.MODEL,
            HostOnlyWorkflowModel::class.java.name,
            HostOnlyWorkflowModel::class.java.name,
        ),
        prototypeEntry(
            TypeRole.MODEL,
            PrototypeHostOnlySubtype::class.java.name,
            PrototypeHostOnlySubtype::class.java.name,
        ),
    )

    @Bean
    open fun prototypeFluxFlowMongoAccess(
        hostTemplate: MongoTemplate,
        databaseFactory: MongoDatabaseFactory,
        @Qualifier("prototypeTypeRegistry") registry: TypeRegistry,
        typeMapperFactory: PrototypeMongoTypeMapperFactory,
    ): PrototypeFluxFlowMongoAccess {
        return PrototypeFluxFlowMongoAccess(
            hostTemplate,
            databaseFactory,
            registry,
            typeMapperFactory,
        )
    }
}
