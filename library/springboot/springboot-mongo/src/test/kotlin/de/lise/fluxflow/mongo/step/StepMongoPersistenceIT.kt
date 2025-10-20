package de.lise.fluxflow.mongo.step

import de.fluxflow.flowquery.query.Query
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.*

private typealias MongoQuery = org.springframework.data.mongodb.core.query.Query

@MongoIntegrationTest
class StepMongoPersistenceIT {
    @Autowired
    lateinit var stepPersistence: StepPersistence
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Test
    fun `find all should support filter, sorting and pagination`() {
        // Act
        val result = stepPersistence.findAll(
            Query.of {
                where {
                    get(StepData::workflowId).isEqual(workflowId1)
                }.sort {
                    get(StepData::version).asc()
                }
            }
        )

        // Assert
        assertThat(result.items).hasSize(1)
    }


    private val workflowId1 = UUID.randomUUID().toString()
    @BeforeEach
    fun setup() {
        mongoTemplate.findAllAndRemove(
            MongoQuery(),
            StepDocument::class.java
        )

        stepPersistence.save(
            StepData(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId1,
                kind = "workflow-kind",
                version = "v1.0.0",
                data = mapOf(
                    "someKey" to 4
                ),
                status = Status.Active,
                metadata = mapOf(),
            )
        )
    }
}