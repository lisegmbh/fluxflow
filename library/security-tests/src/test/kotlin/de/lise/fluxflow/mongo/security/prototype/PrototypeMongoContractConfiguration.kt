package de.lise.fluxflow.mongo.security.prototype

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@TestConfiguration
open class PrototypeMongoContractConfiguration {
    @Bean
    open fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions.create { adapter ->
            adapter.registerConverter(PrototypeConvertedValueWriter)
            adapter.registerConverter(PrototypeConvertedValueReader)
        }
}
