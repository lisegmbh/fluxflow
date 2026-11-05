package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowEnum
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

@Boot4MongoIntegrationTest
@SpringBootTest(properties = ["fluxflow.mongo.enabled=true", "spring.mongodb.representation.uuid=standard"], classes = [
    Boot4MongoIntegrationTestConfig::class, MongoConfiguration::class, ProductionMongoSecurityConfiguration::class,
])
class Boot4ProductionMongoBuiltinValuesIT : AbstractProductionMongoBuiltinValuesIT() {
    @Test
    fun `Jackson 3 Set enum inputs round trip through current and legacy persistence`() {
        val mapper = JsonMapper.builder().build()
        val type = mapper.typeFactory.constructCollectionType(Set::class.java, SecurityTestWorkflowEnum::class.java)
        listOf("[]", "[\"Ready\"]", "[\"Ready\",\"Done\"]").forEach { json ->
            val value = mapper.readValue<Set<SecurityTestWorkflowEnum>>(json, type)
            enumSetRoundTrip(value, "java.util.RegularEnumSet")
        }
    }
}
