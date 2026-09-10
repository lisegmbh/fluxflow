package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.security.fixtures.MODEL_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowModelType
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowSubtype
import de.lise.fluxflow.mongo.security.fixtures.VALUE_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.securityTestModel
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

abstract class AbstractProductionMongoCustomTypeKeyIT {
    @Autowired
    private lateinit var workflows: WorkflowPersistence

    @Autowired
    private lateinit var access: FluxFlowMongoAccess

    @Autowired
    private lateinit var workflowFlowQueries: MongoFlowQueryRepository<WorkflowDocument>

    @Test
    fun `D08 production wiring preserves and guards the configured Mongo type key`() {
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = securityTestModel()
        workflows.create(model, identifier)
        val collection = access.template.getCollection(
            access.template.getCollectionName(WorkflowDocument::class.java)
        )

        val persisted = requireNotNull(collection.find(eq("_id", identifier.value)).first())
        val persistedModel = persisted.get("model", Document::class.java)
        assertThat(persistedModel.getString("@type"))
            .isEqualTo(model.javaClass.name)
        assertThat(persistedModel.containsKey("_class")).isFalse()

        collection.updateOne(
            eq("_id", identifier.value),
            combine(
                set("model.@type", MODEL_TYPE_ALIAS),
                set("model.nested.0.alias.@type", VALUE_TYPE_ALIAS),
            ),
        )

        assertThat(workflows.find(identifier)?.model).isEqualTo(model)
    }

    @Test
    fun `D10 type queries use the configured Mongo type key`() {
        val modelId = WorkflowIdentifier(UUID.randomUUID().toString())
        val subtypeId = WorkflowIdentifier(UUID.randomUUID().toString())
        workflows.create(securityTestModel(), modelId)
        workflows.create(SecurityTestWorkflowSubtype("subtype", "custom type key"), subtypeId)

        val results = workflowFlowQueries.find(WorkflowDocument::class.java) {
            where {
                get(WorkflowDocument::model).isType(SecurityTestWorkflowModelType::class)
            }
        }

        assertThat(results.map { it.id })
            .containsExactlyInAnyOrder(modelId.value, subtypeId.value)
    }
}
