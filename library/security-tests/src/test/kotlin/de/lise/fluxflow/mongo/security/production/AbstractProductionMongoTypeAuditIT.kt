package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.MongoDocumentTypePolicy
import de.lise.fluxflow.mongo.MongoTypeAuditInspectionException
import de.lise.fluxflow.mongo.audit.FluxFlowMongoTypeAudit
import de.lise.fluxflow.mongo.audit.MongoTypeAuditIssue
import de.lise.fluxflow.mongo.audit.MongoTypeAuditOptions
import de.lise.fluxflow.mongo.audit.RawBsonFluxFlowMongoTypeAudit
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
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
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.groups.Tuple.tuple
import org.bson.Document
import org.bson.codecs.configuration.CodecConfigurationException
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mapping.model.MappingInstantiationException
import java.time.Instant
import java.util.UUID

abstract class AbstractProductionMongoTypeAuditIT : AbstractProductionMongoAuditMappingContract(AuditMapping.DEFAULT) {
    protected open val supportsUuidDocumentReferenceWrite: Boolean = true

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
    fun `O03 audit reports a required values container rejected by public persistence`() {
        val workflowId = WorkflowIdentifier("missing-record-values-workflow")
        val data = StepData(
            steps.randomId(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("payload" to "safe value"), Status.Active, emptyMap(),
        )
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", ObjectId(data.id))).first())
        stored.get("dataEntries", Document::class.java).remove("values")
        collection(StepDocument::class.java).replaceOne(Document("_id", ObjectId(data.id)), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable { steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id)) }

        assertThat(readFailure).isInstanceOf(MappingInstantiationException::class.java)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .describedAs("Audit findings after the public read failed with %s", readFailure?.javaClass?.name)
            .singleElement().extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), data.id, "/dataEntries/values", null,
                MongoTypeAuditIssue.MALFORMED, null)
        assertThat(report.isCompatible).isFalse()
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(allVisitedCounts(StepDocument::class.java to 1L))
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 structural map findings do not hide sibling infrastructure type names`() {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        fixture.records.remove("jvmTypes")
        fixture.records.get("types", Document::class.java).get("payload", Document::class.java)["typeName"] = WITNESS_NAME
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        assertUnknownType(catchThrowable { fixture.read() }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.findings).extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(
                tuple(collectionName(fixture.type), fixture.id.toString(), "/dataEntries/jvmTypes", null, MongoTypeAuditIssue.MALFORMED, null),
                tuple(collectionName(fixture.type), fixture.id.toString(), "/dataEntries/types/payload/typeName", TypeRole.VALUE,
                    MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
            )
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RequiredMapCase::class)
    fun `O03 required record maps reject publicly invalid raw forms`(case: RequiredMapCase) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        mutateContainer(fixture.records, case.property, case.form)
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        val readFailure = catchThrowable { fixture.read() }

        assertThat(readFailure).isInstanceOf(RuntimeException::class.java)
        assertStructuralFinding(fixture, "/dataEntries/${case.property}", readFailure)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OptionalRootCase::class)
    fun `O03 absent and null optional record roots retain genuine legacy fallback reads`(case: OptionalRootCase) {
        val fixture = currentRecordFixture(case.field, mapOf("payload" to "safe legacy value"))
        mutateContainer(fixture.source, case.field.property, case.form)
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        fixture.read()
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(report.failures).isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RequiredRootCase::class)
    fun `O03 definition record roots are required independently of legacy fallback roots`(case: RequiredRootCase) {
        val fixture = currentRecordFixture(case.field, mapOf("payload" to "safe metadata"))
        val nested = case.field == RecordField.DEFINITION_DATA_METADATA
        val parent = if (nested) fixture.source.getList("data", Document::class.java).single() else fixture.source
        mutateContainer(parent, "metadata", case.form)
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        val readFailure = catchThrowable { fixture.read() }

        assertThat(readFailure).isInstanceOf(RuntimeException::class.java)
        assertStructuralFinding(fixture, if (nested) "/data/0/metadata" else "/metadata", readFailure)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DefaultEntriesCase::class)
    fun `O03 empty and null records preserve publicly accepted JVM entry defaults`(case: DefaultEntriesCase) {
        val workflowId = WorkflowIdentifier("default-entries-workflow")
        val values: Map<String, Any?> = if (case.nullRecord) mapOf("nullable" to null) else emptyMap()
        val data = StepData(steps.randomId(), workflowId.value, AUDIT_STEP_KIND, "1", values, Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        assertThat(audit.audit().isCompatible).isTrue()
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", ObjectId(data.id))).first())
        val records = stored.get("dataEntries", Document::class.java)
        when (case.form) {
            EntriesDefault.MISSING -> records.get("jvmTypes", Document::class.java).remove("entries")
            EntriesDefault.NULL -> records.get("jvmTypes", Document::class.java)["entries"] = null
            EntriesDefault.EMPTY_JVM_MAP -> records["jvmTypes"] = Document()
        }
        collection(StepDocument::class.java).replaceOne(Document("_id", ObjectId(data.id)), stored)
        val before = rawSnapshot()

        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(report.failures).isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit reports a scalar JVM entry container when public persistence rejects it`() {
        val workflowId = WorkflowIdentifier("scalar-entries-workflow")
        val data = StepData(steps.randomId(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("nullable" to null), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        assertThat(audit.audit().isCompatible).isTrue()
        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", ObjectId(data.id))).first())
        stored.get("dataEntries", Document::class.java).get("jvmTypes", Document::class.java)["entries"] = 42
        collection(StepDocument::class.java).replaceOne(Document("_id", ObjectId(data.id)), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable { steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id)) }

        assertThat(readFailure).isInstanceOf(RuntimeException::class.java)
        val report = audit.audit()
        assertThat(report.complete).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .describedAs("JVM entry audit after public read failed with %s", readFailure?.javaClass?.name)
            .singleElement().extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), data.id, "/dataEntries/jvmTypes/entries", null,
                MongoTypeAuditIssue.MALFORMED, null)
        assertThat(report.isCompatible).isFalse()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class ContainerForm { MISSING, NULL, SCALAR }

    enum class RequiredMapCase(val property: String, val form: ContainerForm) {
        JVM_MISSING("jvmTypes", ContainerForm.MISSING), JVM_NULL("jvmTypes", ContainerForm.NULL), JVM_SCALAR("jvmTypes", ContainerForm.SCALAR),
        VALUES_NULL("values", ContainerForm.NULL), VALUES_SCALAR("values", ContainerForm.SCALAR),
        TYPES_MISSING("types", ContainerForm.MISSING), TYPES_NULL("types", ContainerForm.NULL), TYPES_SCALAR("types", ContainerForm.SCALAR),
    }

    enum class OptionalRootCase(val field: RecordField, val form: ContainerForm) {
        DATA_MISSING(RecordField.STEP_DATA, ContainerForm.MISSING), DATA_NULL(RecordField.STEP_DATA, ContainerForm.NULL),
        META_MISSING(RecordField.STEP_METADATA, ContainerForm.MISSING), META_NULL(RecordField.STEP_METADATA, ContainerForm.NULL),
        PARAMS_MISSING(RecordField.JOB_PARAMETERS, ContainerForm.MISSING), PARAMS_NULL(RecordField.JOB_PARAMETERS, ContainerForm.NULL),
    }

    enum class RequiredRootCase(val field: RecordField, val form: ContainerForm) {
        META_MISSING(RecordField.DEFINITION_METADATA, ContainerForm.MISSING), META_NULL(RecordField.DEFINITION_METADATA, ContainerForm.NULL),
        META_SCALAR(RecordField.DEFINITION_METADATA, ContainerForm.SCALAR),
        DATA_META_MISSING(RecordField.DEFINITION_DATA_METADATA, ContainerForm.MISSING), DATA_META_NULL(RecordField.DEFINITION_DATA_METADATA, ContainerForm.NULL),
        DATA_META_SCALAR(RecordField.DEFINITION_DATA_METADATA, ContainerForm.SCALAR),
    }

    enum class EntriesDefault { MISSING, NULL, EMPTY_JVM_MAP }

    enum class DefaultEntriesCase(val nullRecord: Boolean, val form: EntriesDefault) {
        EMPTY_MISSING(false, EntriesDefault.MISSING), EMPTY_NULL(false, EntriesDefault.NULL), EMPTY_JVM_MAP(false, EntriesDefault.EMPTY_JVM_MAP),
        NULL_RECORD_MISSING(true, EntriesDefault.MISSING), NULL_RECORD_NULL(true, EntriesDefault.NULL), NULL_RECORD_JVM_MAP(true, EntriesDefault.EMPTY_JVM_MAP),
    }

    private fun mutateContainer(parent: Document, property: String, form: ContainerForm) {
        when (form) {
            ContainerForm.MISSING -> parent.remove(property)
            ContainerForm.NULL -> parent[property] = null
            ContainerForm.SCALAR -> parent[property] = 42
        }
    }

    private fun assertStructuralFinding(fixture: CurrentRecordFixture, path: String, readFailure: Throwable?) {
        val report = audit.audit()
        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .describedAs("Structural audit after public read failed with %s", readFailure?.javaClass?.name)
            .singleElement().extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(fixture.type), fixture.id.toString(), path, null, MongoTypeAuditIssue.MALFORMED, null)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReferenceScalarProbe::class)
    fun `O03 public reference scalar conversion preserves matching mappings and uses`(probe: ReferenceScalarProbe) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val use = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java)
        val entry = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single()
        assertThat(entry.getString("reference")).isEqualTo(use.getString("value"))
        entry["reference"] = if (probe.side == ReferenceSide.USE) probe.stringValue else probe.rawValue
        use["value"] = if (probe.side == ReferenceSide.ENTRY) probe.stringValue else probe.rawValue
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        fixture.read()
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(report.failures).isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class ReferenceSide { ENTRY, USE, BOTH }

    enum class ReferenceScalarProbe(val rawValue: Any, val stringValue: String, val side: ReferenceSide) {
        INTEGER_ENTRY(42, "42", ReferenceSide.ENTRY), INTEGER_USE(42, "42", ReferenceSide.USE), INTEGER_BOTH(42, "42", ReferenceSide.BOTH),
        BOOLEAN_ENTRY(true, "true", ReferenceSide.ENTRY), BOOLEAN_USE(true, "true", ReferenceSide.USE), BOOLEAN_BOTH(true, "true", ReferenceSide.BOTH),
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReferenceBsonProbe::class)
    fun `O03 public reference BSON conversion preserves matching mappings and uses`(probe: ReferenceBsonProbe) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val use = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java)
        val entry = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single()
        assertThat(entry.getString("reference")).isEqualTo(use.getString("value"))
        entry["reference"] = if (probe.side == ReferenceSide.USE) probe.stringValue else probe.rawValue()
        use["value"] = if (probe.side == ReferenceSide.ENTRY) probe.stringValue else probe.rawValue()
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        fixture.read()
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(report.failures).isEmpty()
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class ReferenceBsonProbe(val rawValue: () -> Any, val stringValue: String, val side: ReferenceSide) {
        DOCUMENT_ENTRY({ Document() }, "{}", ReferenceSide.ENTRY),
        DOCUMENT_USE({ Document() }, "{}", ReferenceSide.USE),
        DOCUMENT_BOTH({ Document() }, "{}", ReferenceSide.BOTH),
        OBJECT_ID_ENTRY({ ObjectId("000000000000000000000042") }, "000000000000000000000042", ReferenceSide.ENTRY),
        OBJECT_ID_USE({ ObjectId("000000000000000000000042") }, "000000000000000000000042", ReferenceSide.USE),
        OBJECT_ID_BOTH({ ObjectId("000000000000000000000042") }, "000000000000000000000042", ReferenceSide.BOTH),
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DocumentReferenceSide::class)
    fun `O03 Document reference with UUID matches the public Spring conversion in either direction`(side: DocumentReferenceSide) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val use = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java)
        val entry = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single()
        val bsonReference = Document("uuid", UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val springReference = requireNotNull(access.converter.conversionService.convert(bsonReference, String::class.java))
        entry["reference"] = if (side == DocumentReferenceSide.ENTRY_IS_STRING) springReference else bsonReference
        use["value"] = if (side == DocumentReferenceSide.USE_IS_STRING) springReference else bsonReference
        val beforeWrite = rawSnapshot()
        if (!supportsUuidDocumentReferenceWrite) {
            val writeFailure = catchThrowable {
                collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
            }
            assertThat(writeFailure).isInstanceOf(CodecConfigurationException::class.java)
            assertThat(rawSnapshot()).isEqualTo(beforeWrite)
            assertThat(witnessLoader.events).isEmpty()
        } else {
            collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
            val persisted = requireNotNull(collection(fixture.type).find(Document("_id", fixture.id)).first())
            val persistedEntryReference = persisted.get("dataEntries", Document::class.java)
                .get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single().get("reference")
            val persistedUseReference = persisted.get("dataEntries", Document::class.java)
                .get("types", Document::class.java).get("payload", Document::class.java)
                .get("jvmTypeReference", Document::class.java).get("value")
            if (side == DocumentReferenceSide.ENTRY_IS_STRING) {
                assertThat(persistedEntryReference).isInstanceOf(String::class.java)
                assertThat(persistedUseReference).isInstanceOf(Document::class.java)
            } else {
                assertThat(persistedEntryReference).isInstanceOf(Document::class.java)
                assertThat(persistedUseReference).isInstanceOf(String::class.java)
            }
            val before = rawSnapshot()

            fixture.read()
            val report = audit.audit()

            assertThat(springReference).isNotEmpty()
            assertThat(report.complete).isTrue()
            assertThat(report.isCompatible).isTrue()
            assertThat(report.findings).isEmpty()
            assertThat(report.failures).isEmpty()
            assertThat(rawSnapshot()).isEqualTo(before)
            assertThat(witnessLoader.events).isEmpty()
        }
    }

    enum class DocumentReferenceSide { ENTRY_IS_STRING, USE_IS_STRING }

    @Test
    fun `O03 Document reference typeName key remains reference data during legacy type name audit`() {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val use = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java)
        val entry = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single()
        val bsonReference = Document("typeName", "opaque-reference")
        val springReference = requireNotNull(access.converter.conversionService.convert(bsonReference, String::class.java))
        entry["reference"] = bsonReference
        use["value"] = bsonReference
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        val readFailure = catchThrowable { fixture.read() }
        val report = audit.audit()

        assertThat(springReference).isNotEmpty()
        assertThat(readFailure).isInstanceOf(UnknownTypeException::class.java)
        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(fixture.type), fixture.id.toString(),
                "/dataEntries/types/payload/jvmTypeReference/value/typeName", TypeRole.VALUE,
                MongoTypeAuditIssue.UNREGISTERED, "opaque-reference")
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit locates a used JVM reference whose writer mapping was removed`() {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val reference = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java).getString("value")
        val entries = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java)
        assertThat(entries).hasSize(1)
        assertThat(entries.single().getString("reference")).isEqualTo(reference)
        val used = entries.single { it.getString("reference") == reference }
        assertThat(entries.remove(used)).isTrue()
        assertThat(entries).isEmpty()
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        val readFailure = catchThrowable { fixture.read() }

        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .describedAs("Reference audit after public read failed with %s", readFailure?.javaClass?.name)
            .singleElement().extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(fixture.type), fixture.id.toString(),
                "/dataEntries/types/payload/jvmTypeReference/value", null, MongoTypeAuditIssue.MALFORMED, null)
        assertThat(report.isCompatible).isFalse()
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(allVisitedCounts(StepDocument::class.java to 1L))
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit allows unique unused JVM references and rejects duplicate used or unused references`() {
        val unused = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val unusedEntries = unused.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java)
        val extra = Document(unusedEntries.single()).append("reference", "unused-reference")
        unusedEntries.add(extra)
        collection(unused.type).replaceOne(Document("_id", unused.id), unused.source)
        unused.read()

        assertThat(audit.audit().isCompatible).isTrue()

        val usedDuplicate = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "used value"))
        val usedEntries = usedDuplicate.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java)
        usedEntries.add(Document(usedEntries.single()))
        collection(usedDuplicate.type).replaceOne(Document("_id", usedDuplicate.id), usedDuplicate.source)
        val usedReport = audit.audit()

        assertThat(usedReport.isCompatible).isFalse()
        assertThat(usedReport.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(usedDuplicate.type), usedDuplicate.id.toString(),
                "/dataEntries/jvmTypes/entries/1/reference", MongoTypeAuditIssue.MALFORMED)
        collection(usedDuplicate.type).deleteOne(Document("_id", usedDuplicate.id))

        val unusedDuplicate = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "another value"))
        val unusedEntriesWithDuplicate = unusedDuplicate.records.get("jvmTypes", Document::class.java)
            .getList("entries", Document::class.java)
        val unusedEntry = Document(unusedEntriesWithDuplicate.single()).append("reference", "unused-duplicate")
        unusedEntriesWithDuplicate.add(unusedEntry)
        unusedEntriesWithDuplicate.add(Document(unusedEntry))
        collection(unusedDuplicate.type).replaceOne(Document("_id", unusedDuplicate.id), unusedDuplicate.source)
        val unusedReport = audit.audit()

        assertThat(unusedReport.isCompatible).isFalse()
        assertThat(unusedReport.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(unusedDuplicate.type), unusedDuplicate.id.toString(),
                "/dataEntries/jvmTypes/entries/2/reference", MongoTypeAuditIssue.MALFORMED)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReferenceShapeProbe::class)
    fun `O03 public read records missing and null reference behavior before audit`(probe: ReferenceShapeProbe) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val use = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java)
        val entry = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single()
        when (probe) {
            ReferenceShapeProbe.MISSING_ENTRY_REFERENCE -> entry.remove("reference")
            ReferenceShapeProbe.NULL_ENTRY_REFERENCE -> entry["reference"] = null
            ReferenceShapeProbe.MISSING_USE_VALUE -> use.remove("value")
            ReferenceShapeProbe.NULL_USE_VALUE -> use["value"] = null
        }
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)
        val before = rawSnapshot()

        val readFailure = catchThrowable { fixture.read() }
        assertThat(readFailure)
            .describedAs("Public read must reject %s before the audit runs", probe)
            .isNotNull
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        val path = when (probe) {
            ReferenceShapeProbe.MISSING_ENTRY_REFERENCE, ReferenceShapeProbe.NULL_ENTRY_REFERENCE ->
                "/dataEntries/jvmTypes/entries/0/reference"
            ReferenceShapeProbe.MISSING_USE_VALUE, ReferenceShapeProbe.NULL_USE_VALUE ->
                "/dataEntries/types/payload/jvmTypeReference/value"
        }
        val expectedFindings = if (probe == ReferenceShapeProbe.MISSING_ENTRY_REFERENCE ||
            probe == ReferenceShapeProbe.NULL_ENTRY_REFERENCE) {
            listOf(
                tuple(collectionName(fixture.type), fixture.id.toString(), path, MongoTypeAuditIssue.MALFORMED),
                tuple(collectionName(fixture.type), fixture.id.toString(),
                    "/dataEntries/types/payload/jvmTypeReference/value", MongoTypeAuditIssue.MALFORMED),
            )
        } else {
            listOf(tuple(collectionName(fixture.type), fixture.id.toString(), path, MongoTypeAuditIssue.MALFORMED))
        }
        assertThat(report.findings)
            .describedAs("Public read outcome: %s", readFailure?.javaClass?.name ?: "success")
            .extracting("collection", "documentId", "path", "issue")
            .containsExactlyElementsOf(expectedFindings)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class ReferenceShapeProbe {
        MISSING_ENTRY_REFERENCE, NULL_ENTRY_REFERENCE, MISSING_USE_VALUE, NULL_USE_VALUE,
    }

    @Test
    fun `O03 typed records report a missing values key against the public writer baseline`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-records-missing-value")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("payload" to "persisted value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val records = stored.get("dataEntries", Document::class.java)
        records.get("values", Document::class.java).remove("payload")
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/values/payload", MongoTypeAuditIssue.MALFORMED)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 typed records report a missing types key against the public writer baseline`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-records-missing-type")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("payload" to "persisted value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val records = stored.get("dataEntries", Document::class.java)
        records.get("types", Document::class.java).remove("payload")
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/types/payload", MongoTypeAuditIssue.MALFORMED)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 typed record audit reports a missing JVM discriminator`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-record-missing-discriminator")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf(
                "payload" to "persisted value",
                "nullable" to null,
                "nested" to listOf("first", null, listOf("nested")),
            ), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val types = stored.get("dataEntries", Document::class.java).get("types", Document::class.java)
        val payloadType = types.get("payload", Document::class.java)
        assertThat(access.typeKey).isEqualTo("_class")
        assertThat(payloadType).containsKey(access.typeKey)
        payloadType.remove(access.typeKey)
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure)
            .describedAs("Public read outcome for a TypeRecord without its discriminator")
            .isNotNull
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/types/payload/_class", null, MongoTypeAuditIssue.MALFORMED, null)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 typed record audit reports a missing collection type discriminator`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-record-missing-collection-type-discriminator")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf(
                "payload" to "persisted value",
                "nullable" to null,
                "nested" to listOf("first", null, listOf("nested")),
            ), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val types = stored.get("dataEntries", Document::class.java).get("types", Document::class.java)
        val nestedType = types.get("nested", Document::class.java)
        val collectionType = nestedType.get("collectionType", Document::class.java)
        assertThat(access.typeKey).isEqualTo("_class")
        assertThat(collectionType).containsKey(access.typeKey)
        collectionType.remove(access.typeKey)
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure)
            .describedAs("Public read outcome for a collection TypeRecord without its discriminator")
            .isNotNull
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/types/nested/collectionType/_class", null, MongoTypeAuditIssue.MALFORMED, null)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 typed record audit reports a missing component type discriminator`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-record-missing-component-type-discriminator")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf(
                "payload" to "persisted value",
                "nullable" to null,
                "nested" to listOf("first", null, listOf("nested")),
            ), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val types = stored.get("dataEntries", Document::class.java).get("types", Document::class.java)
        val nestedType = types.get("nested", Document::class.java)
        val firstComponent = nestedType.getList("componentTypes", Document::class.java)[0]
        assertThat(access.typeKey).isEqualTo("_class")
        assertThat(firstComponent).containsKey(access.typeKey)
        firstComponent.remove(access.typeKey)
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure)
            .describedAs("Public read outcome for a component TypeRecord without its discriminator")
            .isNotNull
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/types/nested/componentTypes/0/_class", null, MongoTypeAuditIssue.MALFORMED, null)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AddedTypedRecordKeySide::class)
    fun `O03 typed records report unmatched added keys on either side`(side: AddedTypedRecordKeySide) {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-records-added-key-${side.name.lowercase()}")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("payload" to "persisted value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val records = stored.get("dataEntries", Document::class.java)
        val expectedPath = when (side) {
            AddedTypedRecordKeySide.VALUES -> {
                records.get("values", Document::class.java)["extra"] = "untyped value"
                "/dataEntries/types/extra"
            }
            AddedTypedRecordKeySide.TYPES -> {
                val payloadType = records.get("types", Document::class.java).get("payload", Document::class.java)
                records.get("types", Document::class.java)["extra"] = Document(payloadType)
                "/dataEntries/values/extra"
            }
        }
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(), expectedPath,
                MongoTypeAuditIssue.MALFORMED)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class AddedTypedRecordKeySide { VALUES, TYPES }

    @Test
    fun `O03 typed record keyset findings escape application keys as JSON pointers`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-records-escaped-key")
        val applicationKey = "slash/key.tilde"
        val persistedKey = "slash/key~tilde"
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf(applicationKey to "persisted value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val values = stored.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(values).containsKey(persistedKey).doesNotContainKey(applicationKey)
        values.remove(persistedKey)
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(StepDocument::class.java), id.toHexString(),
                "/dataEntries/values/slash~1key~0tilde", MongoTypeAuditIssue.MALFORMED)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 job parameter typed records enforce value and type keysets`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("job-parameter-keyset")
        val data = JobData(id.toHexString(), workflowId.value, AUDIT_JOB_KIND, mapOf("payload" to "persisted value"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(JobDocument::class.java).find(Document("_id", id)).first())
        stored.get("parameterEntries", Document::class.java).get("values", Document::class.java).remove("payload")
        collection(JobDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            jobs.findForWorkflowAndId(workflowId, JobIdentifier(data.id))
        }
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(collectionName(JobDocument::class.java), id.toHexString(),
                "/parameterEntries/values/payload", MongoTypeAuditIssue.MALFORMED)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 typed records report independent data and metadata keyset findings`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-records-sibling-findings")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("dataKey" to "data value"), Status.Active, mapOf("metadataKey" to "metadata value"))
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        stored.get("dataEntries", Document::class.java).get("values", Document::class.java).remove("dataKey")
        stored.get("metadataEntries", Document::class.java).get("values", Document::class.java).remove("metadataKey")
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(
                tuple(collectionName(StepDocument::class.java), id.toHexString(),
                    "/dataEntries/values/dataKey", MongoTypeAuditIssue.MALFORMED),
                tuple(collectionName(StepDocument::class.java), id.toHexString(),
                    "/metadataEntries/values/metadataKey", MongoTypeAuditIssue.MALFORMED),
            )
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 typed record keyset and type-name findings remain independent`() {
        val id = ObjectId()
        val workflowId = WorkflowIdentifier("typed-records-independent-findings")
        val data = StepData(id.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("payload" to "persisted value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))).isEqualTo(data)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)

        val stored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", id)).first())
        val records = stored.get("dataEntries", Document::class.java)
        records["values"] = null
        records.get("types", Document::class.java).get("payload", Document::class.java)["typeName"] = WITNESS_NAME
        collection(StepDocument::class.java).replaceOne(Document("_id", id), stored)
        val before = rawSnapshot()

        assertUnknownType(catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(data.id))
        }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(
                tuple(collectionName(StepDocument::class.java), id.toHexString(),
                    "/dataEntries/types/payload/typeName", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(collectionName(StepDocument::class.java), id.toHexString(),
                    "/dataEntries/values", null, MongoTypeAuditIssue.MALFORMED, null),
            )
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "opaque reference '{0}'")
    @ValueSource(strings = ["opaque-reference", "", "   "])
    fun `O03 arbitrary string references including empty and whitespace remain readable`(reference: String) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val use = fixture.records.get("types", Document::class.java).get("payload", Document::class.java)
            .get("jvmTypeReference", Document::class.java)
        val entry = fixture.records.get("jvmTypes", Document::class.java).getList("entries", Document::class.java).single()
        entry["reference"] = reference
        use["value"] = reference
        collection(fixture.type).replaceOne(Document("_id", fixture.id), fixture.source)

        fixture.read()
        val report = audit.audit()

        assertThat(report.isCompatible).isTrue()
        assertThat(report.findings).isEmpty()
        assertThat(report.failures).isEmpty()
    }

    @Test
    fun `O03 nested collection reference use is audited recursively and isolated from another typed container`() {
        val stepId = ObjectId()
        val workflowId = WorkflowIdentifier("nested-reference-isolation")
        val stepData = StepData(stepId.toHexString(), workflowId.value, AUDIT_STEP_KIND, "1",
            mapOf("nested" to listOf("first", null, listOf("second"))), Status.Active, emptyMap())
        steps.create(stepData)

        val jobId = ObjectId()
        val jobData = JobData(jobId.toHexString(), workflowId.value, AUDIT_JOB_KIND, mapOf("payload" to "other container"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(jobData)

        assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(stepData.id))).isEqualTo(stepData)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(jobData.id))).isEqualTo(jobData)
        assertThat(audit.audit().isCompatible).isTrue()

        val stepStored = requireNotNull(collection(StepDocument::class.java).find(Document("_id", stepId)).first())
        val stepRecords = stepStored.get("dataEntries", Document::class.java)
        val stepEntries = stepRecords.get("jvmTypes", Document::class.java).getList("entries", Document::class.java)
        val stepTypes = stepRecords.get("types", Document::class.java)
        val nestedRecord = stepTypes.get("nested", Document::class.java)
        val nestedUse = nestedRecord.getList("componentTypes", Document::class.java)[2]
            .getList("componentTypes", Document::class.java)[0]
            .get("jvmTypeReference", Document::class.java)
        val nestedReference = nestedUse.getString("value")
        val removed = stepEntries.single { it.getString("reference") == nestedReference }
        assertThat(stepEntries.remove(removed)).isTrue()
        collection(StepDocument::class.java).replaceOne(Document("_id", stepId), stepStored)
        val before = rawSnapshot()
        val readFailure = catchThrowable {
            steps.findForWorkflowAndId(workflowId, StepIdentifier(stepData.id))
        }
        val report = audit.audit()

        assertThat(report.isCompatible).isFalse()
        assertThat(readFailure).isInstanceOf(ValueTypeConversionException::class.java)
        assertThat(report.findings)
            .extracting("collection", "documentId", "path", "issue")
            .containsExactly(
                tuple(collectionName(StepDocument::class.java), stepId.toHexString(),
                    "/dataEntries/types/nested/componentTypes/0/jvmTypeReference/value", MongoTypeAuditIssue.MALFORMED),
                tuple(collectionName(StepDocument::class.java), stepId.toHexString(),
                    "/dataEntries/types/nested/componentTypes/2/componentTypes/0/jvmTypeReference/value",
                    MongoTypeAuditIssue.MALFORMED),
            )
        assertThat(report.findings).noneMatch { finding ->
            finding.collection == collectionName(JobDocument::class.java) && finding.documentId == jobId.toHexString()
        }
        assertThat(rawSnapshot()).isEqualTo(before)
    }

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
    fun `O03 audit stays complete when the final finding exactly reaches its output limit`() {
        val workflowId = WorkflowIdentifier("exact-finding-limit-workflow")
        val workflow = workflows.create(AuditNestedTypes.Model("exact limit"), workflowId)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        val valid = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(valid)
        val stored = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", workflowId.value)).first())
        stored["modelType"] = 42
        collection(WorkflowDocument::class.java).replaceOne(Document("_id", workflowId.value), stored)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        val before = rawSnapshot()

        val report = audit.audit(MongoTypeAuditOptions(maxFindings = 1))

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(WorkflowDocument::class.java), workflowId.value, "/modelType",
                TypeRole.MODEL, MongoTypeAuditIssue.MALFORMED, null)
        val visited = auditedTypes.associate { type ->
            collectionName(type) to if (type == WorkflowDocument::class.java) 1L else 0L
        }.toSortedMap()
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(visited)
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 audit proves an additional finding before capping and does not revisit the cursor`() {
        val workflowId = WorkflowIdentifier("additional-finding-limit-workflow")
        val job = JobData(
            jobs.randomId(), workflowId.value, AUDIT_JOB_KIND, mapOf("argument" to "safe value"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled,
        )
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(job.id))).isEqualTo(job)
        val databaseBefore = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        val name = collectionName(JobDocument::class.java)
        assertThat(name).isEqualTo(auditedTypes.map(::collectionName).min())
        val first = requireNotNull(collection(JobDocument::class.java).find(Document("_id", ObjectId(job.id))).first())
            .apply { this["kind"] = WITNESS_NAME }
        val secondId = ObjectId()
        assertThat(job.id).isLessThan(secondId.toHexString())
        val second = Document.parse(first.toJson()).apply { this["_id"] = secondId }
        val documents = listOf(first, second)
        val before = documents.map { it.toJson() }
        val (source, cursor) = cursorCollection(documents)
        var yielded = 0
        Mockito.doAnswer { documents[yielded++] }.`when`(cursor).next()
        Mockito.doAnswer {
            check(yielded < documents.size) { "Unexpected hasNext after the additional finding" }
            true
        }.`when`(cursor).hasNext()
        val requestedCollections = mutableListOf<String>()
        val cursorAudit = RawBsonFluxFlowMongoTypeAudit(access) { requested ->
            requestedCollections += requested
            if (requested == name) source else access.template.getCollection(requested)
        }

        val report = cursorAudit.audit(MongoTypeAuditOptions(maxFindings = 1))

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(name, job.id, "/kind", TypeRole.JOB, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(mapOf(name to 2L))
        assertThat(requestedCollections).containsExactly(name)
        Mockito.verify(cursor, Mockito.times(2)).next()
        Mockito.verify(cursor, Mockito.times(2)).hasNext()
        Mockito.verify(cursor).close()
        assertThat(documents.map { it.toJson() }).isEqualTo(before)
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 an empty completed audit records every visited collection`() {
        val before = rawSnapshot()

        val report = audit.audit(MongoTypeAuditOptions(batchSize = 1, maxFindings = 1))

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).isEmpty()
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(allVisitedCounts())
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0} findings at limit 2")
    @ValueSource(ints = [1, 2, 3])
    fun `O03 real batch one scans distinguish below exact and exceeded finding limits`(count: Int) {
        val ids = (1..count).map { "batch-one-limit-$it" }
        ids.forEach { id ->
            val identifier = WorkflowIdentifier(id)
            val workflow = workflows.create(AuditNestedTypes.Model(id), identifier)
            assertThat(workflows.find(identifier)).isEqualTo(workflow)
        }
        assertThat(audit.audit().isCompatible).isTrue()
        ids.forEach { id ->
            val stored = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", id)).first())
            stored["modelType"] = 42
            collection(WorkflowDocument::class.java).replaceOne(Document("_id", id), stored)
        }
        val before = rawSnapshot()

        val report = audit.audit(MongoTypeAuditOptions(batchSize = 1, maxFindings = 2))

        assertThat(report.complete).isEqualTo(count <= 2)
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactlyElementsOf(ids.take(2).map { id ->
                tuple(collectionName(WorkflowDocument::class.java), id, "/modelType", TypeRole.MODEL, MongoTypeAuditIssue.MALFORMED, null)
            })
        assertThat(report.scannedDocumentsByCollection)
            .containsExactlyEntriesOf(allVisitedCounts(WorkflowDocument::class.java to count.toLong()))
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LimitSuffix::class)
    fun `O03 scans healthy suffixes and later collections after reaching the finding limit`(suffix: LimitSuffix) {
        val workflowId = WorkflowIdentifier("limit-suffix-workflow")
        val workflow = workflows.create(AuditNestedTypes.Model("later collection"), workflowId)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        val records = (1..3).map { index ->
            JobData(jobs.randomId(), workflowId.value, AUDIT_JOB_KIND, mapOf("argument" to "safe $index"),
                Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        }
        records.forEach { record ->
            jobs.create(record)
            assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(record.id))).isEqualTo(record)
        }
        assertThat(records.map { it.id }).isSorted()
        assertThat(audit.audit().isCompatible).isTrue()
        val first = requireNotNull(collection(JobDocument::class.java).find(Document("_id", ObjectId(records.first().id))).first())
        first["kind"] = WITNESS_NAME
        collection(JobDocument::class.java).replaceOne(Document("_id", first["_id"]), first)
        if (suffix == LimitSuffix.ADDITIONAL_FINDING_IN_LATER_COLLECTION) {
            val later = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", workflowId.value)).first())
            later["modelType"] = 42
            collection(WorkflowDocument::class.java).replaceOne(Document("_id", workflowId.value), later)
        }
        val before = rawSnapshot()

        val report = audit.audit(MongoTypeAuditOptions(batchSize = 1, maxFindings = 1))

        assertThat(report.complete).isEqualTo(suffix == LimitSuffix.HEALTHY)
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(collectionName(JobDocument::class.java), records.first().id, "/kind", TypeRole.JOB,
                MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(
            allVisitedCounts(JobDocument::class.java to 3L, WorkflowDocument::class.java to 1L),
        )
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 multiple findings in one document cap in deterministic pointer order`() {
        val workflowId = WorkflowIdentifier("one-document-multiple-findings")
        val workflow = workflows.create(AuditNestedTypes.Model("safe model"), workflowId)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        assertThat(audit.audit().isCompatible).isTrue()
        val stored = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", workflowId.value)).first())
        stored["modelType"] = 42
        stored.get("model", Document::class.java)["_class"] = WITNESS_NAME
        collection(WorkflowDocument::class.java).replaceOne(Document("_id", workflowId.value), stored)
        val before = rawSnapshot()
        assertThat(audit.audit().findings).extracting("path", "issue")
            .containsExactly(tuple("/model/_class", MongoTypeAuditIssue.UNREGISTERED), tuple("/modelType", MongoTypeAuditIssue.MALFORMED))

        val report = audit.audit(MongoTypeAuditOptions(maxFindings = 1))

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(workflowId.value, "/model/_class", TypeRole.MODEL, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(allVisitedCounts(WorkflowDocument::class.java to 1L))
        assertThat(rawSnapshot()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LimitDriverFailure::class)
    fun `O03 driver failures after the output limit retain findings and visited counts`(failure: LimitDriverFailure) {
        val workflowId = WorkflowIdentifier("limit-driver-workflow")
        val job = JobData(jobs.randomId(), workflowId.value, AUDIT_JOB_KIND, mapOf("argument" to "safe value"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(job.id))).isEqualTo(job)
        val databaseBefore = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        val first = requireNotNull(collection(JobDocument::class.java).find(Document("_id", ObjectId(job.id))).first())
            .apply { this["kind"] = WITNESS_NAME }
        val capped = failure == LimitDriverFailure.CLOSE_AFTER_ADDITIONAL_FINDING
        val documents = if (capped) listOf(first, Document.parse(first.toJson()).apply { this["_id"] = ObjectId() }) else listOf(first)
        val before = documents.map { it.toJson() }
        val driverFailure = if (failure == LimitDriverFailure.HAS_NEXT_AFTER_LIMIT) DriverFailure.HAS_NEXT else DriverFailure.CLOSE
        val (source, cursor) = cursorCollection(documents, driverFailure)
        val name = collectionName(JobDocument::class.java)
        assertThat(name).isEqualTo(auditedTypes.map(::collectionName).min())
        val requestedCollections = mutableListOf<String>()
        val cursorAudit = RawBsonFluxFlowMongoTypeAudit(access) { requested ->
            requestedCollections += requested
            if (requested == name) source else access.template.getCollection(requested)
        }

        val report = cursorAudit.audit(MongoTypeAuditOptions(maxFindings = 1))

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).singleElement().extracting("collection", "cause")
            .containsExactly(name, "IllegalArgumentException: driver $driverFailure failed")
        assertThat(report.findings).singleElement()
            .extracting("documentId", "path", "issue", "persistedName")
            .containsExactly(job.id, "/kind", MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        val expectedCounts = if (capped) sortedMapOf(name to 2L) else allVisitedCounts(JobDocument::class.java to 1L)
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(expectedCounts)
        assertThat(requestedCollections).containsExactlyElementsOf(expectedCounts.keys)
        Mockito.verify(cursor, Mockito.times(if (capped) 2 else 1)).next()
        Mockito.verify(cursor, Mockito.times(2)).hasNext()
        Mockito.verify(cursor).close()
        assertThat(documents.map { it.toJson() }).isEqualTo(before)
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 a budget finding at the output limit keeps a completed suffix incomplete`() {
        val workflowId = WorkflowIdentifier("limit-budget-workflow")
        val job = JobData(jobs.randomId(), workflowId.value, AUDIT_JOB_KIND, mapOf("argument" to "safe value"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(job.id))).isEqualTo(job)
        val databaseBefore = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        val bad = requireNotNull(collection(JobDocument::class.java).find(Document("_id", ObjectId(job.id))).first())
        val healthy = Document.parse(bad.toJson()).apply { this["_id"] = ObjectId() }
        var nested = Document("leaf", "safe value")
        repeat(105) { nested = Document("next", nested) }
        bad["deep"] = nested
        val documents = listOf(bad, healthy)
        val before = documents.map { it.toJson() }
        val (source, cursor) = cursorCollection(documents)
        val name = collectionName(JobDocument::class.java)
        val cursorAudit = RawBsonFluxFlowMongoTypeAudit(access) { requested ->
            if (requested == name) source else access.template.getCollection(requested)
        }

        val report = cursorAudit.audit(MongoTypeAuditOptions(maxFindings = 1))

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement().extracting("documentId", "path", "issue")
            .containsExactly(job.id, "/deep" + "/next".repeat(100), MongoTypeAuditIssue.MALFORMED)
        assertThat(report.scannedDocumentsByCollection).containsExactlyEntriesOf(allVisitedCounts(JobDocument::class.java to 2L))
        Mockito.verify(cursor, Mockito.times(2)).next()
        Mockito.verify(cursor, Mockito.times(3)).hasNext()
        Mockito.verify(cursor).close()
        assertThat(documents.map { it.toJson() }).isEqualTo(before)
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(InvalidAuditOption::class)
    fun `O03 audit options reject nonpositive batch sizes and finding limits`(option: InvalidAuditOption) {
        val failure = catchThrowable {
            when (option) {
                InvalidAuditOption.ZERO_BATCH -> MongoTypeAuditOptions(batchSize = 0)
                InvalidAuditOption.NEGATIVE_BATCH -> MongoTypeAuditOptions(batchSize = -1)
                InvalidAuditOption.ZERO_LIMIT -> MongoTypeAuditOptions(maxFindings = 0)
                InvalidAuditOption.NEGATIVE_LIMIT -> MongoTypeAuditOptions(maxFindings = -1)
            }
        }

        assertThat(failure).isExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    enum class LimitSuffix { HEALTHY, ADDITIONAL_FINDING_IN_LATER_COLLECTION }

    enum class LimitDriverFailure { HAS_NEXT_AFTER_LIMIT, CLOSE_AFTER_LIMIT, CLOSE_AFTER_ADDITIONAL_FINDING }

    enum class InvalidAuditOption { ZERO_BATCH, NEGATIVE_BATCH, ZERO_LIMIT, NEGATIVE_LIMIT }

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
    fun `O03 audit continues after an individual document exceeds the inspection budget`() {
        val workflowId = WorkflowIdentifier("budget-audit-workflow")
        val workflow = workflows.create(AuditNestedTypes.Model("budget sentinel"), workflowId)
        val job = JobData(
            jobs.randomId(), workflowId.value, AUDIT_JOB_KIND, mapOf("argument" to "safe value"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled,
        )
        jobs.create(job)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(job.id))).isEqualTo(job)
        val databaseBefore = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)

        val firstCollection = collectionName(JobDocument::class.java)
        val sentinelCollection = collectionName(WorkflowDocument::class.java)
        assertThat(firstCollection).isEqualTo(auditedTypes.map(::collectionName).min())
        assertThat(firstCollection).isLessThan(sentinelCollection)
        val valid = requireNotNull(collection(JobDocument::class.java).find(Document("_id", ObjectId(job.id))).first())
        val badId = ObjectId()
        val bad = Document.parse(valid.toJson()).apply {
            this["_id"] = badId
            var nested = Document("leaf", "safe value")
            repeat(105) { nested = Document("next", nested) }
            this["deep"] = nested
        }
        val unknownId = ObjectId()
        val unknown = Document.parse(valid.toJson()).apply {
            this["_id"] = unknownId
            this["kind"] = WITNESS_NAME
        }
        val sentinel = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", workflowId.value)).first())
            .apply { this["modelType"] = WITNESS_NAME }
        val documents = listOf(valid, bad, unknown, sentinel)
        val before = documents.map { it.toJson() }
        val (first, firstCursor) = cursorCollection(listOf(valid, bad, unknown))
        val (last, lastCursor) = cursorCollection(listOf(sentinel))
        val cursorAudit = RawBsonFluxFlowMongoTypeAudit(access) { name ->
            when (name) {
                firstCollection -> first
                sentinelCollection -> last
                else -> access.template.getCollection(name)
            }
        }

        val report = cursorAudit.audit()

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.findings)
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactlyInAnyOrder(
                tuple(firstCollection, badId.toHexString(), "/deep" + "/next".repeat(100), null, MongoTypeAuditIssue.MALFORMED, null),
                tuple(firstCollection, unknownId.toHexString(), "/kind", TypeRole.JOB, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(sentinelCollection, workflowId.value, "/modelType", TypeRole.MODEL, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
            )
        assertThat(report.failures).isEmpty()
        assertThat(report.scannedDocumentsByCollection[firstCollection]).isEqualTo(3L)
        assertThat(report.scannedDocumentsByCollection[sentinelCollection]).isEqualTo(1L)
        assertThat(report.scannedDocumentsByCollection.values.sum()).isEqualTo(4L)
        Mockito.verify(firstCursor, Mockito.times(3)).next()
        Mockito.verify(lastCursor).next()
        Mockito.verify(firstCursor).close()
        Mockito.verify(lastCursor).close()
        assertThat(documents.map { it.toJson() }).isEqualTo(before)
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `O03 ordinary type findings leave a fully inspected audit complete`() {
        val workflowId = WorkflowIdentifier("complete-type-finding")
        val workflow = workflows.create(AuditNestedTypes.Model("ordinary finding"), workflowId)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        assertThat(audit.audit().isCompatible).isTrue()
        val stored = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", workflowId.value)).first())
        stored["modelType"] = WITNESS_NAME
        val before = stored.toJson()
        val databaseBefore = rawSnapshot()
        val (source, cursor) = cursorCollection(listOf(stored))
        val name = collectionName(WorkflowDocument::class.java)

        val report = RawBsonFluxFlowMongoTypeAudit(access) { requested ->
            if (requested == name) source else access.template.getCollection(requested)
        }.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(workflowId.value, "/modelType", TypeRole.MODEL, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(report.scannedDocumentsByCollection[name]).isEqualTo(1L)
        Mockito.verify(cursor).close()
        assertThat(stored.toJson()).isEqualTo(before)
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(InspectionBudgetCase::class)
    fun `O03 audit budget boundaries retain structured pointers and existing guard exceptions`(case: InspectionBudgetCase) {
        val atLimit: Document
        val overLimit: Document
        val depth: Int
        val nodes: Int
        val expectedPath: String
        when (case) {
            InspectionBudgetCase.DEPTH -> {
                atLimit = Document("a/~", Document("b/~", "leaf"))
                overLimit = Document("a/~", Document("b/~", Document("c/~", "leaf")))
                depth = 2
                nodes = 100
                expectedPath = "/a~1~0/b~1~0/c~1~0"
            }
            InspectionBudgetCase.ROOT_NODES -> {
                atLimit = Document("first", 1).append("second", 2)
                overLimit = Document(atLimit).append("overflow/~", 3)
                depth = 100
                nodes = 3
                expectedPath = "/overflow~1~0"
            }
            InspectionBudgetCase.MAP_NODES -> {
                atLimit = Document("pending", 0).append("box", Document("first", 1).append("second", 2))
                overLimit = Document("pending", 0).append("box", Document("first", 1).append("second", 2).append("overflow/~", 3))
                depth = 100
                nodes = 5
                expectedPath = "/box/overflow~1~0"
            }
            InspectionBudgetCase.LIST_NODES -> {
                atLimit = Document("pending", 0).append("items", listOf(1, 2))
                overLimit = Document("pending", 0).append("items", listOf(1, 2, 3))
                depth = 100
                nodes = 5
                expectedPath = "/items/2"
            }
            InspectionBudgetCase.ARRAY_NODES -> {
                atLimit = Document("pending", 0).append("items", arrayOf(1, 2))
                overLimit = Document("pending", 0).append("items", arrayOf(1, 2, 3))
                depth = 100
                nodes = 5
                expectedPath = "/items/2"
            }
        }
        atLimit[access.typeKey] = WorkflowDocument::class.java.name
        overLimit[access.typeKey] = WorkflowDocument::class.java.name
        val before = listOf(bsonShape(atLimit), bsonShape(overLimit))
        val policy = MongoDocumentTypePolicy(
            access.typeAliases, access.typeKey, depth, nodes, access.fieldNames, access.valueTypes::isRegistered,
        )
        val atProjection = Document(atLimit).apply { remove(access.typeKey) }
        val overProjection = Document(overLimit).apply { remove(access.typeKey) }

        assertThat(policy.auditAliases(WorkflowDocument::class.java, atLimit, access.valueTypes::isRegistered)).isEmpty()
        policy.validate(WorkflowDocument::class.java, atLimit)
        policy.validateProjection(BudgetProjection::class.java, atProjection)
        val inspectionFailure = catchThrowable {
            policy.auditAliases(WorkflowDocument::class.java, overLimit, access.valueTypes::isRegistered)
        }

        assertThat(inspectionFailure).isExactlyInstanceOf(MongoTypeAuditInspectionException::class.java)
        assertThat((inspectionFailure as MongoTypeAuditInspectionException).path).isEqualTo(expectedPath)
        assertThat(catchThrowable { policy.validate(WorkflowDocument::class.java, overLimit) })
            .isExactlyInstanceOf(IllegalArgumentException::class.java)
        assertThat(catchThrowable { policy.validateProjection(BudgetProjection::class.java, overProjection) })
            .isExactlyInstanceOf(IllegalArgumentException::class.java)
        assertThat(listOf(bsonShape(atLimit), bsonShape(overLimit))).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DriverFailure::class)
    fun `O03 external driver failures remain collection failures and release acquired cursors`(failure: DriverFailure) {
        val workflowId = WorkflowIdentifier("driver-failure-workflow")
        val workflow = workflows.create(AuditNestedTypes.Model("driver sentinel"), workflowId)
        val job = JobData(
            jobs.randomId(), workflowId.value, AUDIT_JOB_KIND, mapOf("argument" to "safe value"),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled,
        )
        jobs.create(job)
        assertThat(workflows.find(workflowId)).isEqualTo(workflow)
        assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(job.id))).isEqualTo(job)
        val databaseBefore = rawSnapshot()
        assertThat(audit.audit().isCompatible).isTrue()
        val firstName = collectionName(JobDocument::class.java)
        val lastName = collectionName(WorkflowDocument::class.java)
        assertThat(firstName).isEqualTo(auditedTypes.map(::collectionName).min())
        assertThat(firstName).isLessThan(lastName)
        val valid = requireNotNull(collection(JobDocument::class.java).find(Document("_id", ObjectId(job.id))).first())
        val next = Document.parse(valid.toJson()).apply { this["_id"] = ObjectId() }
        val records = if (failure in setOf(DriverFailure.HAS_NEXT, DriverFailure.NEXT)) listOf(valid, next) else listOf(valid)
        val sentinel = requireNotNull(collection(WorkflowDocument::class.java).find(Document("_id", workflowId.value)).first())
            .apply { this["modelType"] = WITNESS_NAME }
        val before = (records + sentinel).map { it.toJson() }
        val (first, firstCursor) = cursorCollection(records, failure)
        val (last, lastCursor) = cursorCollection(listOf(sentinel))
        val cursorAudit = RawBsonFluxFlowMongoTypeAudit(access) { name ->
            when (name) {
                firstName -> {
                    if (failure == DriverFailure.PROVIDER) throw IllegalArgumentException("driver $failure failed")
                    first
                }
                lastName -> last
                else -> access.template.getCollection(name)
            }
        }

        val report = cursorAudit.audit()

        assertThat(report.complete).isFalse()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).singleElement().extracting("collection", "cause")
            .containsExactly(firstName, "IllegalArgumentException: driver $failure failed")
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(lastName, workflowId.value, "/modelType", TypeRole.MODEL, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        val acquired = failure in setOf(DriverFailure.HAS_NEXT, DriverFailure.NEXT, DriverFailure.CLOSE)
        val attempted = if (acquired) 1L else 0L
        assertThat(report.scannedDocumentsByCollection[firstName]).isEqualTo(attempted)
        assertThat(report.scannedDocumentsByCollection[lastName]).isEqualTo(1L)
        assertThat(report.scannedDocumentsByCollection.values.sum()).isEqualTo(attempted + 1L)
        assertThat(report.scannedDocumentsByCollection).containsOnlyKeys(*auditedTypes.map(::collectionName).toTypedArray())
        Mockito.verify(firstCursor, Mockito.times(if (acquired) 1 else 0)).close()
        val nextCalls = when (failure) {
            DriverFailure.NEXT -> 2
            DriverFailure.HAS_NEXT, DriverFailure.CLOSE -> 1
            else -> 0
        }
        Mockito.verify(firstCursor, Mockito.times(nextCalls)).next()
        Mockito.verify(lastCursor).next()
        Mockito.verify(lastCursor).close()
        assertThat((records + sentinel).map { it.toJson() }).isEqualTo(before)
        assertThat(rawSnapshot()).isEqualTo(databaseBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    enum class InspectionBudgetCase { DEPTH, ROOT_NODES, MAP_NODES, LIST_NODES, ARRAY_NODES }

    enum class DriverFailure { PROVIDER, FIND, SORT, BATCH, ITERATOR, HAS_NEXT, NEXT, CLOSE }

    private data class BudgetProjection(val payload: Any?)

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

    @Suppress("UNCHECKED_CAST")
    private fun cursorCollection(
        documents: List<Document>,
        failure: DriverFailure? = null,
    ): Pair<MongoCollection<Document>, MongoCursor<Document>> {
        val collection = Mockito.mock(MongoCollection::class.java) as MongoCollection<Document>
        val find = Mockito.mock(FindIterable::class.java) as FindIterable<Document>
        val cursor = Mockito.mock(MongoCursor::class.java) as MongoCursor<Document>
        val iterator = documents.iterator()
        var delivered = 0
        val driverException = IllegalArgumentException("driver $failure failed")
        Mockito.doReturn(find).`when`(collection).find()
        Mockito.doReturn(find).`when`(find).sort(Document("_id", 1))
        Mockito.doReturn(find).`when`(find).batchSize(500)
        Mockito.doReturn(cursor).`when`(find).iterator()
        Mockito.doAnswer {
            if (failure == DriverFailure.HAS_NEXT && delivered == 1) throw driverException
            iterator.hasNext()
        }.`when`(cursor).hasNext()
        Mockito.doAnswer {
            if (failure == DriverFailure.NEXT && delivered == 1) throw driverException
            iterator.next().also { delivered++ }
        }.`when`(cursor).next()
        when (failure) {
            DriverFailure.FIND -> Mockito.doThrow(driverException).`when`(collection).find()
            DriverFailure.SORT -> Mockito.doThrow(driverException).`when`(find).sort(Document("_id", 1))
            DriverFailure.BATCH -> Mockito.doThrow(driverException).`when`(find).batchSize(500)
            DriverFailure.ITERATOR -> Mockito.doThrow(driverException).`when`(find).iterator()
            DriverFailure.CLOSE -> Mockito.doThrow(driverException).`when`(cursor).close()
            else -> Unit
        }
        return collection to cursor
    }

    private fun bsonShape(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { (key, nested) -> key to bsonShape(nested) }
        is Iterable<*> -> value.map(::bsonShape)
        is Array<*> -> value.map(::bsonShape)
        else -> value
    }

    private fun typedRecords(typeName: Any?): Document {
        val records = Document()
        access.converter.write(TypedRecords.fromData(mapOf("value" to "persisted value")), records)
        val entries = (records["jvmTypes"] as Document)["entries"] as List<*>
        (entries.single() as Document)["type"] = typeName
        return records
    }

    private fun allVisitedCounts(vararg counts: Pair<Class<*>, Long>): java.util.SortedMap<String, Long> {
        val byType = counts.toMap()
        return auditedTypes.associate { type -> collectionName(type) to (byType[type] ?: 0L) }.toSortedMap()
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
