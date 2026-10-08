package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.springframework.boot.test.context.SpringBootTest

@MongoIntegrationTest
@SpringBootTest(
    properties = [
        "fluxflow.mongo.enabled=true",
        "spring.data.mongodb.auto-index-creation=true",
    ],
    classes = [
        IntegrationTestConfig::class,
        MongoConfiguration::class,
        PrototypeMongoHostTemplateFixture::class,
        Boot3PrototypeMongoAdapterConfiguration::class,
    ],
)
class Boot3PrototypeMongoHostTemplateIT : AbstractPrototypeMongoHostTemplateContractIT()
