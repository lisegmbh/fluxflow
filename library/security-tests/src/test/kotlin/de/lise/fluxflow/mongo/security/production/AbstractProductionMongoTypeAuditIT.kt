package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.audit.FluxFlowMongoTypeAudit
import de.lise.fluxflow.mongo.audit.MongoTypeAuditIssue
import de.lise.fluxflow.mongo.audit.MongoTypeAuditOptions
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

abstract class AbstractProductionMongoTypeAuditIT {
    @Autowired
    private lateinit var access: FluxFlowMongoAccess

    @Autowired
    private lateinit var audit: FluxFlowMongoTypeAudit

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
    fun `O03 audit marks capped results incomplete and reports malformed metadata`() {
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

    private fun typedRecords(typeName: String): Document = Document(
        "jvmTypes",
        Document(
            "entries",
            listOf(Document("reference", "type-1").append("type", typeName)),
        ),
    )

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
