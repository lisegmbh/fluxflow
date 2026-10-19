package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.audit.FluxFlowMongoTypeAudit
import de.lise.fluxflow.mongo.audit.MongoTypeAuditIssue
import de.lise.fluxflow.mongo.audit.MongoTypeAuditOptions
import de.lise.fluxflow.mongo.audit.RawBsonFluxFlowMongoTypeAudit
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.fixtures.AUDIT_JOB_KIND
import de.lise.fluxflow.mongo.security.fixtures.AUDIT_STEP_KIND
import de.lise.fluxflow.mongo.security.fixtures.AuditNestedTypes
import de.lise.fluxflow.mongo.security.fixtures.VALUE_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.assertUnknownType
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.definition.DataDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.groups.Tuple.tuple
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.time.Instant

abstract class AbstractProductionMongoTypeAuditIT : AbstractProductionMongoAuditMappingContract(AuditMapping.DEFAULT) {
    @Autowired
    private lateinit var access: FluxFlowMongoAccess

    @Autowired
    private lateinit var audit: FluxFlowMongoTypeAudit

    @Autowired
    private lateinit var workflows: WorkflowPersistence

    @Autowired
    private lateinit var steps: StepPersistence

    @Autowired
    private lateinit var jobs: JobPersistence

    @Autowired
    private lateinit var stepDefinitions: StepDefinitionPersistence

    @Autowired
    @Qualifier("productionWitnessClassLoader")
    private lateinit var witnessLoader: WitnessClassLoader

    @BeforeEach
    fun clearCollections() {
        assertThat(witnessLoader.events).isEmpty()
        auditedTypes.forEach { type ->
            collection(type).deleteMany(Document())
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CurrentInfrastructureLocation::class)
    fun `O03 current type names remain guarded throughout existing infrastructure maps and lists`(location: CurrentInfrastructureLocation) {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("nested-current-record-workflow")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("record" to "scalar", "collection" to listOf("first", null, listOf("nested"))), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val types = stored.get("dataEntries", Document::class.java).get("types", Document::class.java)
        val scalar = types.get("record", Document::class.java)
        val collectionRecord = types.get("collection", Document::class.java)
        val components = collectionRecord.getList("componentTypes", Document::class.java)
        val (target, path) = when (location) {
            CurrentInfrastructureLocation.REFERENCE -> {
                val reference = scalar.get("jvmTypeReference", Document::class.java)
                assertThat(reference).containsKey("value")
                reference to "/dataEntries/types/record/jvmTypeReference/typeName"
            }
            CurrentInfrastructureLocation.COLLECTION_RECORD -> collectionRecord to "/dataEntries/types/collection/typeName"
            CurrentInfrastructureLocation.COLLECTION_CONTAINER -> collectionRecord.get("collectionType", Document::class.java) to
                "/dataEntries/types/collection/collectionType/typeName"
            CurrentInfrastructureLocation.NULL_COMPONENT -> components[1] to "/dataEntries/types/collection/componentTypes/1/typeName"
            CurrentInfrastructureLocation.NESTED_COLLECTION -> components[2].getList("componentTypes", Document::class.java)[0] to
                "/dataEntries/types/collection/componentTypes/2/componentTypes/0/typeName"
            CurrentInfrastructureLocation.EXTRA_LIST_MAP -> {
                val extra = Document()
                scalar["extra"] = listOf(extra)
                extra to "/dataEntries/types/record/extra/0/typeName"
            }
        }
        target["typeName"] = WITNESS_NAME
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        assertUnknownType(catchThrowable { steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id)) }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(), path,
                TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 null records keep inner type checks when JVM entries use their empty default`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("null-current-record-workflow")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("nullable" to null), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val records = stored.get("dataEntries", Document::class.java)
        records.get("jvmTypes", Document::class.java).remove("entries")
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)
        records.get("types", Document::class.java).get("nullable", Document::class.java)["typeName"] = WITNESS_NAME
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        assertUnknownType(catchThrowable { steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id)) }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("path", "expectedRole", "issue", "persistedName")
            .containsExactly("/dataEntries/types/nullable/typeName", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class CurrentInfrastructureLocation { REFERENCE, COLLECTION_RECORD, COLLECTION_CONTAINER, NULL_COMPONENT, NESTED_COLLECTION, EXTRA_LIST_MAP }

    @Test
    fun `O03 audit reports extra type names inside current typed record metadata`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-record-typeName-workflow")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("payload" to "current value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)
        assertThat(witnessLoader.events).isEmpty()
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        stored.get("dataEntries", Document::class.java).get("types", Document::class.java)
            .get("payload", Document::class.java)["typeName"] = WITNESS_NAME
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        assertUnknownType(catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/types/payload/typeName", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit accepts a legacy application key named typeName`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("legacy-typeName-workflow")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("typeName" to "application value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        stored.remove("dataEntries")
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val before = rawSnapshot()

        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(report.isCompatible).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit reports persisted type deviations without hydration or writes`() {
        val workflowId = "audit-workflow"
        val stepId = ObjectId()
        val jobId = ObjectId()
        val definitionId = ObjectId()

        collection(WorkflowDocument::class.java).insertOne(
            Document("_id", workflowId)
                .append("_class", WorkflowDocument::class.java.name)
                .append("modelType", WITNESS_NAME)
                .append("model", Document("_class", WITNESS_NAME)),
        )
        collection(StepDocument::class.java).insertOne(
            Document("_id", stepId)
                .append("_class", StepDocument::class.java.name)
                .append("kind", WITNESS_NAME)
                .append(
                    "dataTypeMap",
                    Document("legacy", Document("typeName", WITNESS_NAME)),
                )
                .append("dataEntries", typedRecords(WITNESS_NAME)),
        )
        collection(JobDocument::class.java).insertOne(
            Document("_id", jobId)
                .append("_class", JobDocument::class.java.name)
                .append("kind", WITNESS_NAME)
                .append(
                    "parameterTypeMap",
                    Document("legacy", Document("typeName", WITNESS_NAME)),
                )
                .append("parameterEntries", typedRecords(WITNESS_NAME)),
        )
        collection(StepDefinitionDocument::class.java).insertOne(
            Document("_id", definitionId)
                .append("_class", StepDefinitionDocument::class.java.name)
                .append("kind", WITNESS_NAME)
                .append("metadata", typedRecords(WITNESS_NAME))
                .append(
                    "data",
                    listOf(Document("metadata", typedRecords(WITNESS_NAME))),
                ),
        )
        val before = rawSnapshot()

        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.scannedDocumentsByCollection.values.sum()).isEqualTo(4)
        assertThat(report.findings)
            .extracting(
                "collection",
                "documentId",
                "path",
                "expectedRole",
                "issue",
                "persistedName",
            )
            .containsExactly(
                tuple(collectionName(JobDocument::class.java), jobId.toHexString(), "/kind", TypeRole.JOB, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(JobDocument::class.java), jobId.toHexString(), "/parameterEntries/jvmTypes/entries/0/type", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(JobDocument::class.java), jobId.toHexString(), "/parameterTypeMap/legacy/typeName", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDefinitionDocument::class.java), definitionId.toHexString(), "/data/0/metadata/jvmTypes/entries/0/type", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDefinitionDocument::class.java), definitionId.toHexString(), "/kind", TypeRole.STEP, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDefinitionDocument::class.java), definitionId.toHexString(), "/metadata/jvmTypes/entries/0/type", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDocument::class.java), stepId.toHexString(), "/dataEntries/jvmTypes/entries/0/type", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDocument::class.java), stepId.toHexString(), "/dataTypeMap/legacy/typeName", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDocument::class.java), stepId.toHexString(), "/kind", TypeRole.STEP, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(WorkflowDocument::class.java), workflowId, "/model/_class", TypeRole.MODEL, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(WorkflowDocument::class.java), workflowId, "/modelType", TypeRole.MODEL, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
            )
        assertThat(witnessLoader.events)
            .describedAs("The raw audit must not load, initialize or construct persisted types")
            .isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
    }

    @Test
    fun `O03 audit accepts registered Mongo aliases and built-in value types`() {
        val id = "clean-workflow"
        val stepId = ObjectId()
        val registeredModel = "de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowModel"
        val registeredValue = "de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowValue"
        collection(WorkflowDocument::class.java).insertOne(
            Document("_id", id)
                .append("_class", WorkflowDocument::class.java.name)
                .append("modelType", registeredModel)
                .append(
                    "model",
                    Document("_class", registeredModel)
                        .append("value", Document("_class", registeredValue)),
                ),
        )
        collection(StepDocument::class.java).insertOne(
            Document("_id", stepId)
                .append("_class", StepDocument::class.java.name)
                .append("kind", AUDIT_STEP_KIND)
                .append("dataEntries", typedRecords(String::class.java.name)),
        )
        val before = rawSnapshot()

        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit accepts documents round-tripped through public persistence APIs`() {
        val workflowId = WorkflowIdentifier("audit-public-round-trip")
        val model = AuditNestedTypes.Model("persisted model")
        val workflow = workflows.create(model, workflowId)
        val step = StepData(
            steps.randomId(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("text" to "persisted data", "nullable" to null, "list" to listOf("one", "two")),
            Status.Active, mapOf("label" to "step metadata"),
        )
        val job = JobData(
            jobs.randomId(), workflowId.value, AUDIT_JOB_KIND,
            mapOf("argument" to "persisted argument"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled,
        )
        val definition = StepDefinitionData(
            AUDIT_STEP_KIND, "1", mapOf("label" to "definition metadata"),
            listOf(
                DataDefinitionData(
                    "text", String::class.java.name, mapOf("label" to "data metadata"), false,
                ),
            ),
        )
        steps.create(step)
        jobs.create(job)
        stepDefinitions.save(definition)

        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(step.id))).isEqualTo(step)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(job.id))).isEqualTo(job)
        assertThat(stepDefinitions.findForKindAndVersion(AUDIT_STEP_KIND, "1")).isEqualTo(definition)
        val before = rawSnapshot()

        val report = audit.audit()

        assertThat(report.isCompatible).isTrue()
        assertThat(report.scannedDocumentsByCollection.values.sum()).isEqualTo(4L)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit accepts the qualified name written for a nested model`() {
        val id = "nested-model"
        val model = AuditNestedTypes.Model("nested")
        access.template.save(
            WorkflowDocument(id, model, model::class.qualifiedName)
        )
        val stored = requireNotNull(
            collection(WorkflowDocument::class.java).find(Document("_id", id)).first()
        )

        assertThat(stored.getString("modelType")).isEqualTo(AuditNestedTypes.Model::class.qualifiedName)
        assertThat((stored["model"] as Document).getString("_class"))
            .isEqualTo(AuditNestedTypes.Model::class.java.name)
        assertThat(audit.audit().isCompatible).isTrue()
    }

    @Test
    fun `O03 audit accepts fixed scalar and container workflow models`() {
        val models = listOf<Any?>(
            null,
            "scalar",
            listOf("one", 2, null),
            linkedMapOf("string" to "value", "null" to null),
        )
        models.forEachIndexed { index, model ->
            access.template.save(
                WorkflowDocument(
                    "fixed-model-$index",
                    model,
                    model?.let { it::class.qualifiedName },
                )
            )
        }

        assertThat(audit.audit().isCompatible).isTrue()
    }

    @Test
    fun `O03 audit classifies malformed and disallowed metadata`() {
        val stepId = ObjectId()
        collection(StepDocument::class.java).insertOne(
            Document("_id", stepId)
                .append("_class", StepDocument::class.java.name)
                .append("kind", VALUE_TYPE_ALIAS)
                .append("dataEntries", typedRecords(42)),
        )

        val report = audit.audit()

        assertThat(report.findings)
            .extracting("path", "expectedRole", "issue", "persistedName")
            .containsExactly(
                tuple("/dataEntries/jvmTypes/entries/0/type", TypeRole.VALUE, MongoTypeAuditIssue.MALFORMED, null),
                tuple("/kind", TypeRole.STEP, MongoTypeAuditIssue.DISALLOWED, VALUE_TYPE_ALIAS),
            )
    }

    @Test
    fun `O03 audit rejects missing required kinds and typed record type`() {
        val stepCollection = collectionName(StepDocument::class.java)
        val jobCollection = collectionName(JobDocument::class.java)
        val definitionCollection = collectionName(StepDefinitionDocument::class.java)
        collection(StepDocument::class.java).insertOne(
            Document("_id", ObjectId())
                .append("_class", StepDocument::class.java.name)
                .append(
                    "dataEntries",
                    typedRecords(String::class.java.name).apply {
                        val entries = (this["jvmTypes"] as Document)["entries"] as List<*>
                        (entries.single() as Document).remove("type")
                    },
                ),
        )
        collection(JobDocument::class.java).insertOne(
            Document("_id", ObjectId())
                .append("_class", JobDocument::class.java.name),
        )
        collection(StepDefinitionDocument::class.java).insertOne(
            Document("_id", ObjectId())
                .append("_class", StepDefinitionDocument::class.java.name)
                .append("metadata", typedRecords(String::class.java.name)),
        )

        val report = audit.audit()

        assertThat(report.isCompatible).isFalse()
        assertThat(report.findings)
            .extracting("collection", "path", "expectedRole", "issue", "persistedName")
            .containsExactlyInAnyOrder(
                tuple(stepCollection, "/dataEntries/jvmTypes/entries/0/type", TypeRole.VALUE, MongoTypeAuditIssue.MALFORMED, null),
                tuple(stepCollection, "/kind", TypeRole.STEP, MongoTypeAuditIssue.MALFORMED, null),
                tuple(jobCollection, "/kind", TypeRole.JOB, MongoTypeAuditIssue.MALFORMED, null),
                tuple(definitionCollection, "/kind", TypeRole.STEP, MongoTypeAuditIssue.MALFORMED, null),
            )
    }

    @Test
    fun `O03 audit marks capped results incomplete`() {
        collection(WorkflowDocument::class.java).insertOne(
            Document("_id", "limited-workflow")
                .append("_class", WorkflowDocument::class.java.name)
                .append("modelType", 42)
                .append("model", Document("_class", WITNESS_NAME)),
        )

        val report = audit.audit(MongoTypeAuditOptions(maxFindings = 1))

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.findings).hasSize(1)
    }

    @Test
    fun `O03 audit reports collection failures and continues scanning`() {
        val failedCollection = collectionName(JobDocument::class.java)
        val continuedCollection = collectionName(WorkflowDocument::class.java)
        assertThat(failedCollection).isLessThan(continuedCollection)
        collection(WorkflowDocument::class.java).insertOne(
            Document("_id", "scanned-after-failure")
                .append("_class", WorkflowDocument::class.java.name),
        )
        val failingAudit = RawBsonFluxFlowMongoTypeAudit(access) { collectionName ->
            if (collectionName == failedCollection) {
                throw IllegalStateException("audit read failed")
            }
            access.template.getCollection(collectionName)
        }

        val report = failingAudit.audit()

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.findings).isEmpty()
        assertThat(report.failures)
            .extracting("collection", "cause")
            .containsExactly(tuple(failedCollection, "IllegalStateException: audit read failed"))
        assertThat(report.scannedDocumentsByCollection[failedCollection]).isZero()
        assertThat(report.scannedDocumentsByCollection[continuedCollection]).isEqualTo(1L)
    }

    @Test
    fun `O05 audit reports inconsistent registered model metadata`() {
        val registeredModel = "de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowModel"
        val registeredSubtype = "de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowSubtype"
        collection(WorkflowDocument::class.java).insertOne(
            Document("_id", "inconsistent-workflow")
                .append("_class", WorkflowDocument::class.java.name)
                .append("modelType", registeredModel)
                .append("model", Document("_class", registeredSubtype)),
        )

        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.findings)
            .singleElement()
            .extracting("path", "expectedRole", "issue", "persistedName")
            .containsExactly(
                "/modelType",
                TypeRole.MODEL,
                MongoTypeAuditIssue.INCONSISTENT,
                registeredModel,
            )
    }

    private fun typedRecords(typeName: Any?): Document {
        val records = Document()
        access.converter.write(TypedRecords.fromData(mapOf("value" to "persisted value")), records)
        val entries = (records["jvmTypes"] as Document)["entries"] as List<*>
        (entries.single() as Document)["type"] = typeName
        return records
    }

    private fun rawSnapshot(): Map<String, List<String>> = auditedTypes.associate { type ->
        val name = collectionName(type)
        name to collection(type).find().map { it.toJson() }.sorted()
    }

    private fun collectionName(type: Class<*>): String = access.template.getCollectionName(type)

    private fun collection(type: Class<*>) = access.template.getCollection(collectionName(type))

    private companion object {
        val auditedTypes = listOf(
            WorkflowDocument::class.java,
            StepDocument::class.java,
            JobDocument::class.java,
            StepDefinitionDocument::class.java,
        )
    }
}
