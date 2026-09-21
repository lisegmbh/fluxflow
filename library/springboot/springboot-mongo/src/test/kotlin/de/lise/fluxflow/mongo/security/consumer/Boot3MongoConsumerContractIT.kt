package de.lise.fluxflow.mongo.security.consumer

import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.springframework.boot.test.context.SpringBootTest

@MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        IntegrationTestConfig::class,
        MongoConfiguration::class,
        WfmsLikeConsumerConfiguration::class,
    ],
)
class Boot3MongoConsumerContractIT : AbstractMongoConsumerContractIT()
