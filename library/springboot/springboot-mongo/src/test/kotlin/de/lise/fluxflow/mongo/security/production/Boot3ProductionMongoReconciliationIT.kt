package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import org.springframework.boot.test.context.SpringBootTest

@MongoIntegrationTest
@SpringBootTest(
    properties = [
        "fluxflow.mongo.enabled=true",
        "fluxflow.scheduling.reconcileOnStartup=true",
    ],
    classes = [
        IntegrationTestConfig::class,
        BasicConfiguration::class,
        MongoConfiguration::class,
        ProductionMongoReconciliationConfiguration::class,
    ],
)
class Boot3ProductionMongoReconciliationIT : AbstractProductionMongoReconciliationIT()
