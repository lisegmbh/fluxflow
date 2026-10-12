package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.migration.MigrationError
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.fixtures.*
import de.lise.fluxflow.persistence.job.*
import de.lise.fluxflow.persistence.step.*
import de.lise.fluxflow.persistence.step.definition.*
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.*
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import java.time.Instant
import java.util.UUID

abstract class AbstractProductionMongoPathFieldsIT {
    @Autowired private lateinit var workflows: WorkflowPersistence
    @Autowired private lateinit var jobs: JobPersistence
    @Autowired private lateinit var references: ScheduledJobReferencePersistence
    @Autowired private lateinit var steps: StepPersistence
    @Autowired private lateinit var definitions: StepDefinitionPersistence
    @Autowired private lateinit var host: MongoTemplate
    @Autowired @Qualifier("productionWitnessClassLoader") private lateinit var witness: WitnessClassLoader
    @Autowired @Qualifier("migrateToTypeRecordsBootstrapAction") private lateinit var migration: BootstrapAction

    @BeforeEach fun clear() {
        listOf(WorkflowDocument::class.java, JobDocument::class.java, StepDocument::class.java, StepDefinitionDocument::class.java)
            .forEach { collection(it).deleteMany(Document()) }
        assertThat(witness.events).isEmpty()
    }

    @Test fun `host written path workflow model round trips and rejects wrong role and unknown aliases`() {
        val id = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = securityTestModel()
        workflows.create(model, id)
        val raw = requireNotNull(collection(WorkflowDocument::class.java).find(eq("_id", id.value)).first())
        assertThat(raw.get("payload", Document::class.java).get("model", Document::class.java))
            .containsEntry("_class", model.javaClass.name)
        assertThat(workflows.find(id)?.model).isEqualTo(model)
        listOf(VALUE_TYPE_ALIAS, WITNESS_NAME).forEach { alias ->
            collection(WorkflowDocument::class.java).updateOne(eq("_id", id.value), set("payload.model._class", alias))
            assertUnknownType(catchThrowable { workflows.find(id) }, TypeRole.MODEL, alias)
        }
        assertThat(witness.events).isEmpty()
    }

    @Test fun `raw scheduled references read nested mapped workflow identifiers`() {
        val data = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "path-job", emptyMap(),
            Instant.parse("2026-10-05T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id))).isEqualTo(data)
        val raw = requireNotNull(collection(JobDocument::class.java).find(eq("_id", ObjectId(data.id))).first())
        assertThat(raw.get("routing", Document::class.java)).containsEntry("workflow_id", data.workflowId)
        val found = references.findScheduledJobReferences()
        assertThat(found.map { it.jobIdentifier.value }).containsExactly(data.id)
        assertThat(found.map { it.workflowIdentifier.value }).containsExactly(data.workflowId)
    }

    @Test fun `path record containers and definition metadata round trip and keep nested value role`() {
        val step = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), "path-step", "1",
            mapOf("payload" to SecurityTestWorkflowValue("data")), Status.Active,
            mapOf("payload" to SecurityTestWorkflowValue("metadata")))
        steps.create(step)
        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))).isEqualTo(step)
        val definition = StepDefinitionData("path-definition", "1", mapOf("payload" to SecurityTestWorkflowValue("top")),
            listOf(DataDefinitionData("payload", String::class.java.name,
                mapOf("payload" to SecurityTestWorkflowValue("child")), false)))
        definitions.save(definition)
        assertThat(definitions.findForKindAndVersion(definition.kind, definition.version)).isEqualTo(definition)
        collection(StepDocument::class.java).updateOne(eq("_id", ObjectId(step.id)),
            set("records.dataEntries.record.values.payload", Document("_class", MODEL_TYPE_ALIAS)))
        assertUnknownType(catchThrowable {
            steps.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))
        }, TypeRole.VALUE, MODEL_TYPE_ALIAS)
        assertThat(witness.events).isEmpty()
    }

    @Test fun `path namespaces never authorize their own type metadata`() {
        val id = WorkflowIdentifier(UUID.randomUUID().toString())
        workflows.create(securityTestModel(), id)
        collection(WorkflowDocument::class.java).updateOne(eq("_id", id.value), set("payload._class", MODEL_TYPE_ALIAS))
        assertThatThrownBy { workflows.find(id) }.isInstanceOf(IllegalArgumentException::class.java)
        collection(WorkflowDocument::class.java).updateOne(eq("_id", id.value), unset("payload._class"))
        collection(WorkflowDocument::class.java).updateOne(eq("_id", id.value), set("payload.unmapped._class", WITNESS_NAME))
        assertThatThrownBy { workflows.find(id) }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Mongo type metadata is not allowed")
        assertThat(witness.events).isEmpty()
    }

    @Test fun `literal dotted keys remain literal while nested aliases stay value guarded`() {
        val id = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = SecurityLiteralKeyModel(SecurityTestWorkflowValue("literal"))
        workflows.create(model, id)
        val raw = requireNotNull(collection(WorkflowDocument::class.java).find(eq("_id", id.value)).first())
        val embedded = raw.get("payload", Document::class.java).get("model", Document::class.java)
        assertThat(embedded).containsKey("literal.payload").doesNotContainKey("literal")
        assertThat(workflows.find(id)?.model).isEqualTo(model)
        embedded.get("literal.payload", Document::class.java)["_class"] = MODEL_TYPE_ALIAS
        collection(WorkflowDocument::class.java).replaceOne(eq("_id", id.value), raw)
        assertUnknownType(catchThrowable { workflows.find(id) }, TypeRole.VALUE, MODEL_TYPE_ALIAS)
        assertThat(witness.events).isEmpty()
    }

    private fun collection(type: Class<*>) = host.getCollection(host.getCollectionName(type))

    @Test @ExtendWith(OutputCaptureExtension::class)
    fun `legacy migration failure reports the actual nested workflow identifier and preserves BSON`(output: CapturedOutput) {
        val data = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "path-legacy", emptyMap(),
            Instant.parse("2026-10-05T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        val collection = collection(JobDocument::class.java)
        collection.updateOne(eq("_id", ObjectId(data.id)), unset("parameterEntries"))
        collection.updateOne(eq("_id", ObjectId(data.id)), set("parameters.payload", Document("_class", WITNESS_NAME)))
        val before = collection.find(eq("_id", ObjectId(data.id))).first()
        assertThatThrownBy { migration.setup() }.isInstanceOf(MigrationError::class.java).hasMessageContaining(data.id)
        assertThat(output.all).contains("to the workflow '${data.workflowId}'")
        assertThat(collection.find(eq("_id", ObjectId(data.id))).first()).isEqualTo(before)
        assertThat(witness.events).isEmpty()
    }
}

@TestConfiguration
open class PathMongoFieldsConfiguration {
    companion object {
        @Bean @JvmStatic fun pathMongoFields(): BeanPostProcessor = object : BeanPostProcessor {
            override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
                if (bean is MongoMappingContext) bean.setFieldNamingStrategy { property ->
                    val type = property.owner.type
                    when {
                        type == WorkflowDocument::class.java && property.name == "model" -> "payload.model"
                        type == JobDocument::class.java && property.name == "workflowId" -> "routing.workflow_id"
                        type == StepDocument::class.java && property.name in setOf("dataEntries", "metadataEntries") -> "records.${property.name}"
                        type == TypedRecords::class.java -> "record.${property.name}"
                        type == StepDefinitionDocument::class.java && property.name == "data" -> "schema.data"
                        type == StepDefinitionDocument::class.java && property.name == "metadata" -> "records.metadata"
                        type == DataDefinitionDocument::class.java && property.name == "metadata" -> "definition.metadata"
                        else -> property.name
                    }
                }
                return bean
            }
        }
    }
}
