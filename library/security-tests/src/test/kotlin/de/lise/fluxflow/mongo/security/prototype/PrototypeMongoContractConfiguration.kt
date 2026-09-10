package de.lise.fluxflow.mongo.security.prototype

import com.mongodb.ReadPreference
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
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
    open fun prototypeFluxFlowMongoAccess(
        hostTemplate: MongoTemplate,
        databaseFactory: MongoDatabaseFactory,
        @Qualifier("prototypeTypeRegistry") registry: TypeRegistry,
        typeMapperFactory: PrototypeMongoTypeMapperFactory,
    ): PrototypeFluxFlowMongoAccess {
        hostTemplate.setReadPreference(ReadPreference.secondaryPreferred())
        return PrototypeFluxFlowMongoAccess(
            hostTemplate,
            databaseFactory,
            registry,
            typeMapperFactory,
        )
    }
}
