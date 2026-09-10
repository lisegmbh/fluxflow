package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        IntegrationTestConfig::class,
        MongoConfiguration::class,
        PrototypeMongoContractConfiguration::class,
    ],
)
class Boot3PrototypeMongoSecurityIT : AbstractPrototypeMongoSecurityContractIT() {
    override fun typeMapperFactory(): PrototypeMongoTypeMapperFactory =
        VersionSpecificPrototypeMongoTypeMapperFactory()
}
