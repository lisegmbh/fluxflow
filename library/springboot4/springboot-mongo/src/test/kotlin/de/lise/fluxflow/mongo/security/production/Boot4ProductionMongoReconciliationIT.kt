package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import org.springframework.boot.test.context.SpringBootTest

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = [
        "fluxflow.mongo.enabled=true",
        "fluxflow.scheduling.reconcileOnStartup=true",
    ],
    classes = [
        Boot4MongoIntegrationTestConfig::class,
        BasicConfiguration::class,
        MongoConfiguration::class,
        ProductionMongoReconciliationConfiguration::class,
    ],
)
class Boot4ProductionMongoReconciliationIT : AbstractProductionMongoReconciliationIT()
