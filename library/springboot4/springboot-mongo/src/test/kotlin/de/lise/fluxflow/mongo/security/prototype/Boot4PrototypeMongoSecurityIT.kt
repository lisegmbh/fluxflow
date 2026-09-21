package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.e2e.Boot4MongoE2ETestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        Boot4MongoIntegrationTestConfig::class,
        Boot4MongoE2ETestConfiguration::class,
        MongoConfiguration::class,
        PrototypeMongoContractConfiguration::class,
        Boot4PrototypeMongoAdapterConfiguration::class,
    ],
)
class Boot4PrototypeMongoSecurityIT : AbstractPrototypeMongoSecurityContractIT()

@TestConfiguration
open class Boot4PrototypeMongoAdapterConfiguration {
    @Bean
    open fun prototypeMongoTypeMapperFactory(): PrototypeMongoTypeMapperFactory =
        VersionSpecificPrototypeMongoTypeMapperFactory()
}
