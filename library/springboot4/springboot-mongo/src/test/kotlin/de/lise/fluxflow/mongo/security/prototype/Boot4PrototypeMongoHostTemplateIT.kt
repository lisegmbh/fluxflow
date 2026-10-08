package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.e2e.Boot4MongoE2ETestConfiguration
import org.springframework.boot.test.context.SpringBootTest

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = [
        "fluxflow.mongo.enabled=true",
        "spring.data.mongodb.auto-index-creation=true",
    ],
    classes = [
        Boot4MongoIntegrationTestConfig::class,
        Boot4MongoE2ETestConfiguration::class,
        MongoConfiguration::class,
        PrototypeMongoHostTemplateFixture::class,
        Boot4PrototypeMongoAdapterConfiguration::class,
    ],
)
class Boot4PrototypeMongoHostTemplateIT : AbstractPrototypeMongoHostTemplateContractIT()
