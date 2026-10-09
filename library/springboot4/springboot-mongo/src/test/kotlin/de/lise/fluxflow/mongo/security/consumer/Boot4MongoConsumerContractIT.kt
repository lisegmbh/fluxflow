package de.lise.fluxflow.mongo.security.consumer

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        Boot4MongoIntegrationTestConfig::class,
        MongoConfiguration::class,
        WfmsLikeConsumerConfiguration::class,
    ],
)
class Boot4MongoConsumerContractIT : AbstractMongoConsumerContractIT()
