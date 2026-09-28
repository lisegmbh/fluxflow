package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.springframework.boot.test.context.SpringBootTest

@MongoIntegrationTest
@SpringBootTest(
    properties = [
        "fluxflow.mongo.enabled=true",
        "fluxflow.security.test.type-key=@type",
    ],
    classes = [
        IntegrationTestConfig::class,
        MongoConfiguration::class,
        ProductionMongoSecurityConfiguration::class,
    ],
)
class Boot3ProductionMongoCustomTypeKeyIT : AbstractProductionMongoCustomTypeKeyIT()
