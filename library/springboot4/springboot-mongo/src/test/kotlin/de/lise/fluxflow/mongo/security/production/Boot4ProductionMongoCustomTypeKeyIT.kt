package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = [
        "fluxflow.mongo.enabled=true",
        "fluxflow.security.test.type-key=@type",
    ],
    classes = [
        Boot4MongoIntegrationTestConfig::class,
        MongoConfiguration::class,
        ProductionMongoSecurityConfiguration::class,
    ],
)
class Boot4ProductionMongoCustomTypeKeyIT : AbstractProductionMongoCustomTypeKeyIT()
