package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        IntegrationTestConfig::class,
        MongoConfiguration::class,
        PrototypeMongoContractConfiguration::class,
        Boot3PrototypeMongoAdapterConfiguration::class,
    ],
)
class Boot3PrototypeMongoSecurityIT : AbstractPrototypeMongoSecurityContractIT()

@TestConfiguration
open class Boot3PrototypeMongoAdapterConfiguration {
    @Bean
    open fun prototypeMongoTypeMapperFactory(): PrototypeMongoTypeMapperFactory =
        VersionSpecificPrototypeMongoTypeMapperFactory()
}
