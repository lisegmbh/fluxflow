package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.prototype.assertUnknownType
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.UUID

abstract class AbstractProductionMongoSecurityContractIT {
    @Autowired
    private lateinit var hostTemplate: MongoTemplate

    @Autowired
    private lateinit var fluxFlowMongoAccess: FluxFlowMongoAccess

    @Autowired
    private lateinit var workflows: WorkflowPersistence

    @Autowired
    @Qualifier("productionWitnessClassLoader")
    private lateinit var witnessLoader: WitnessClassLoader

    @BeforeEach
    fun clearWorkflows() {
        assertThat(witnessLoader.events).isEmpty()
        if (!hostTemplate.collectionExists(WorkflowDocument::class.java)) {
            hostTemplate.createCollection(WorkflowDocument::class.java)
        }
        hostTemplate.remove(Query(), WorkflowDocument::class.java)
    }

    @Test
    fun `R03 D01 D07 production workflow reads reject an unregistered model before materialization`() {
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val raw = Document("_id", identifier.value)
            .append("model", Document("_class", WITNESS_NAME))
            .append("modelType", WITNESS_NAME)
            .append("_class", WorkflowDocument::class.java.name)
        fluxFlowMongoAccess.template
            .getCollection(fluxFlowMongoAccess.template.getCollectionName(WorkflowDocument::class.java))
            .insertOne(raw)
        assertThat(witnessLoader.events).isEmpty()

        val failure = runCatching { workflows.find(identifier) }.exceptionOrNull()
            ?: throw AssertionError("Expected the production Mongo read to fail closed")

        assertUnknownType(failure, TypeRole.MODEL, WITNESS_NAME)
        assertThat(witnessLoader.events)
            .describedAs("R03 must reject the model type before initializing or constructing it")
            .isEmpty()
        assertThat(
            fluxFlowMongoAccess.template
                .getCollection(fluxFlowMongoAccess.template.getCollectionName(WorkflowDocument::class.java))
                .countDocuments(eq("_id", identifier.value))
        ).isEqualTo(1)
    }
}
