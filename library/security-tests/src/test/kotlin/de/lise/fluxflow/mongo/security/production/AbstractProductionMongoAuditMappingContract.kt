package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.audit.FluxFlowMongoTypeAudit
import de.lise.fluxflow.mongo.audit.MongoTypeAuditIssue
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.fixtures.*
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.time.Instant
import java.util.UUID

/** Public audit/persistence contracts shared by the existing host-mapping contexts. */
abstract class AbstractProductionMongoAuditMappingContract(private val mapping: AuditMapping) {
    @Autowired private lateinit var mappingAccess: FluxFlowMongoAccess
    @Autowired private lateinit var mappingAudit: FluxFlowMongoTypeAudit
    @Autowired private lateinit var mappingSteps: StepPersistence
    @Autowired private lateinit var mappingJobs: JobPersistence
    @Autowired private lateinit var mappingDefinitions: StepDefinitionPersistence
    @Autowired private lateinit var mappingWorkflows: WorkflowPersistence
    @Autowired @Qualifier("productionWitnessClassLoader") private lateinit var mappingWitness: WitnessClassLoader

    @BeforeEach
    fun clearAuditMappingDocuments() {
        roots.forEach { collection(it).deleteMany(Document()) }
        assertThat(mappingWitness.events).isEmpty()
    }

    @Test
    fun `O03 mapped required record containers match public read failures`() {
        val field = when (mapping) {
            AuditMapping.DEFAULT -> RecordField.STEP_METADATA
            AuditMapping.SNAKE -> RecordField.JOB_PARAMETERS
            AuditMapping.PATH -> RecordField.DEFINITION_DATA_METADATA
            AuditMapping.CUSTOM -> RecordField.DEFINITION_METADATA
        }
        val fixture = currentRecordFixture(field, mapOf("payload" to "safe value"))
        val path = when (mapping) {
            AuditMapping.DEFAULT -> {
                setMapped(fixture.records, TypedRecords::class.java, "types", null)
                "/metadataEntries/types"
            }
            AuditMapping.SNAKE -> {
                removeMapped(fixture.records, TypedRecords::class.java, "jvmTypes")
                "/parameter_entries/jvm_types"
            }
            AuditMapping.PATH -> {
                setMapped(fixture.records, TypedRecords::class.java, "values", 42)
                "/schema/data/0/definition/metadata/record/values"
            }
            AuditMapping.CUSTOM -> {
                removeMapped(fixture.source, StepDefinitionDocument::class.java, "metadata")
                "/metadata"
            }
        }
        collection(fixture.type).replaceOne(eq("_id", fixture.id), fixture.source)
        val before = inventorySnapshot()

        assertThat(catchThrowable { fixture.read() }).isInstanceOf(RuntimeException::class.java)
        assertFinding(fixture.type, fixture.id.toString(), path, null, MongoTypeAuditIssue.MALFORMED, null)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RecordField::class, names = ["STEP_DATA", "STEP_METADATA", "JOB_PARAMETERS", "DEFINITION_METADATA", "DEFINITION_DATA_METADATA"])
    fun `O03 current record type maps retain exact inner type findings at every persistence position`(field: RecordField) {
        val fixture = currentRecordFixture(field, mapOf("typeName" to "first", "type_name" to "second"))
        val key = if (mapping == AuditMapping.SNAKE) "type_name" else "typeName"
        val names = mapped(fixture.records, TypedRecords::class.java, "types") as Document
        setMapped(names.get(key, Document::class.java), SimpleType::class.java, "typeName", WITNESS_NAME)
        collection(fixture.type).replaceOne(eq("_id", fixture.id), fixture.source)
        val before = inventorySnapshot()

        assertUnknownType(catchThrowable { fixture.read() }, TypeRole.VALUE, WITNESS_NAME)
        assertFinding(fixture.type, fixture.id.toString(), fixture.typesPath + "/$key/" + extraNameField,
            TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ExtraTypeNameForm::class)
    fun `O03 current record extra type names preserve exact classification and permitted forms`(form: ExtraTypeNameForm) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("typeName" to "first", "type_name" to "second"))
        val key = if (mapping == AuditMapping.SNAKE) "type_name" else "typeName"
        val value: Any? = when (form) {
            ExtraTypeNameForm.UNKNOWN -> WITNESS_NAME
            ExtraTypeNameForm.WHITESPACE -> " "
            ExtraTypeNameForm.EMPTY -> ""
            ExtraTypeNameForm.NULL -> null
            ExtraTypeNameForm.NONSTRING -> 42
            ExtraTypeNameForm.WRONG_ROLE -> MODEL_TYPE_ALIAS
            ExtraTypeNameForm.REGISTERED -> VALUE_TYPE_ALIAS
            ExtraTypeNameForm.BUILTIN -> String::class.java.name
            ExtraTypeNameForm.MISSING -> null
        }
        if (form != ExtraTypeNameForm.MISSING) {
            val names = mapped(fixture.records, TypedRecords::class.java, "types") as Document
            setMapped(names.get(key, Document::class.java), SimpleType::class.java, "typeName", value)
            collection(fixture.type).replaceOne(eq("_id", fixture.id), fixture.source)
        }
        val before = inventorySnapshot()
        val issue = when (form) {
            ExtraTypeNameForm.UNKNOWN, ExtraTypeNameForm.WHITESPACE -> MongoTypeAuditIssue.UNREGISTERED
            ExtraTypeNameForm.EMPTY, ExtraTypeNameForm.NULL, ExtraTypeNameForm.NONSTRING -> MongoTypeAuditIssue.MALFORMED
            ExtraTypeNameForm.WRONG_ROLE -> MongoTypeAuditIssue.DISALLOWED
            else -> null
        }
        if (issue == null) {
            fixture.read()
            assertCleanInventory()
        } else {
            val failure = catchThrowable { fixture.read() }
            if (issue == MongoTypeAuditIssue.MALFORMED) {
                assertThat(generateSequence(failure) { it.cause }.toList()).anyMatch {
                    it is IllegalArgumentException && it.message == "Mongo JVM type name must be a non-empty string"
                }
            } else {
                assertUnknownType(failure, TypeRole.VALUE, value as String)
            }
            assertFinding(fixture.type, fixture.id.toString(), fixture.typesPath + "/$key/" + extraNameField,
                TypeRole.VALUE, issue, value as? String)
        }
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["slash/key", "literal.key"])
    fun `O03 current record application key pointers preserve actual literal BSON segments`(key: String) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf(key to "value"))
        val encoded = if (key == "literal.key") "literal~key" else "slash/key"
        val pointer = if (key == "literal.key") "literal~0key" else "slash~1key"
        val names = mapped(fixture.records, TypedRecords::class.java, "types") as Document
        assertThat(names).containsKey(encoded)
        setMapped(names.get(encoded, Document::class.java), SimpleType::class.java, "typeName", WITNESS_NAME)
        collection(fixture.type).replaceOne(eq("_id", fixture.id), fixture.source)
        val before = inventorySnapshot()

        assertUnknownType(catchThrowable { fixture.read() }, TypeRole.VALUE, WITNESS_NAME)
        assertFinding(fixture.type, fixture.id.toString(), fixture.typesPath + "/$pointer/" + extraNameField,
            TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    private val extraNameField: String get() = if (mapping == AuditMapping.SNAKE) "type_name" else "typeName"

    protected fun currentRecordFixture(field: RecordField, values: Map<String, String>): CurrentRecordFixture {
        val id = ObjectId()
        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val (type, read) = when (field) {
            RecordField.STEP_DATA, RecordField.STEP_METADATA -> {
                val data = StepData(id.toHexString(), workflow.value, AUDIT_STEP_KIND, "1",
                    if (field == RecordField.STEP_DATA) values else emptyMap(), Status.Active,
                    if (field == RecordField.STEP_METADATA) values else emptyMap())
                mappingSteps.create(data)
                StepDocument::class.java to { assertThat(mappingSteps.findForWorkflowAndId(workflow, StepIdentifier(data.id))).isEqualTo(data); Unit }
            }
            RecordField.JOB_PARAMETERS -> {
                val data = JobData(id.toHexString(), workflow.value, AUDIT_JOB_KIND, values,
                    Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
                mappingJobs.create(data)
                JobDocument::class.java to { assertThat(mappingJobs.findForWorkflowAndId(workflow, JobIdentifier(data.id))).isEqualTo(data); Unit }
            }
            RecordField.DEFINITION_METADATA, RecordField.DEFINITION_DATA_METADATA -> {
                val data = StepDefinitionData(AUDIT_STEP_KIND, "1",
                    if (field == RecordField.DEFINITION_METADATA) values else emptyMap(),
                    listOf(DataDefinitionData("payload", String::class.java.name,
                        if (field == RecordField.DEFINITION_DATA_METADATA) values else emptyMap(), false)))
                mappingDefinitions.save(data)
                StepDefinitionDocument::class.java to { assertThat(mappingDefinitions.findForKindAndVersion(data.kind, data.version)).isEqualTo(data); Unit }
            }
            else -> error("The current record fixture requires a current record position")
        }
        read()
        assertCleanInventory()
        val storedId = if (type == StepDefinitionDocument::class.java) raw(type, Document())["_id"] else id
        val source = raw(type, Document("_id", storedId))
        val records = if (field == RecordField.DEFINITION_DATA_METADATA) {
            val data = mapped(source, type, "data") as List<*>
            mapped(data.single() as Document, DataDefinitionDocument::class.java, "metadata") as Document
        } else mapped(source, type, field.property) as Document
        // These are declared fixture-schema literals, independent of the production field resolver.
        val typesPath = mapping.recordPaths[field.ordinal].substringBefore("/jvm") + "/types"
        return CurrentRecordFixture(type, requireNotNull(storedId), read, source, records, typesPath)
    }

    protected data class CurrentRecordFixture(
        val type: Class<*>, val id: Any, val read: () -> Unit,
        val source: Document, val records: Document, val typesPath: String,
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("legacyApplicationKeyCases")
    fun `O03 legacy application keys preserve inner type checks through public fallback reads`(
        position: LegacyPosition,
        mutation: LegacyMutation,
    ) {
        val values: Map<String, Any?> = linkedMapOf(
            "typeName" to "application value", "type_name" to "mapped application value",
            "collectionType" to listOf("one", null, listOf("two")), "componentTypes" to "application components",
            "nullable" to null, "slash/key" to "slash", "literal.key" to "literal dot",
        )
        val id = ObjectId()
        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val (type, read) = if (position == LegacyPosition.JOB_PARAMETERS) {
            val data = JobData(id.toHexString(), workflow.value, AUDIT_JOB_KIND, values,
                Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
            mappingJobs.create(data)
            JobDocument::class.java to { assertThat(mappingJobs.findForWorkflowAndId(workflow, JobIdentifier(data.id))).isEqualTo(data); Unit }
        } else {
            val data = StepData(id.toHexString(), workflow.value, AUDIT_STEP_KIND, "1",
                if (position == LegacyPosition.STEP_DATA) values else emptyMap(), Status.Active,
                if (position == LegacyPosition.STEP_METADATA) values.filterValues { it != null }.mapValues { requireNotNull(it.value) } else emptyMap())
            mappingSteps.create(data)
            StepDocument::class.java to { assertThat(mappingSteps.findForWorkflowAndId(workflow, StepIdentifier(data.id))).isEqualTo(data); Unit }
        }
        read()
        val source = raw(type, Document("_id", id))
        val recordParts = mappingAccess.fieldNames.fieldParts(type, position.currentProperty)
        val recordParent = recordParts.dropLast(1).fold(source) { parent, segment -> parent.get(segment, Document::class.java) }
        recordParent.remove(recordParts.last())
        collection(type).replaceOne(eq("_id", id), source)
        read()
        assertCleanInventory()
        val legacy = mapped(source, type, position.legacyProperty) as Document
        assertThat(legacy).containsKeys("collectionType", "componentTypes", "slash/key", "literal~key")
        val collisionKey = if (mapping == AuditMapping.SNAKE) "type_name" else "typeName"
        val scalarName = if (mapping == AuditMapping.SNAKE) "type_name" else "typeName"
        val collectionComponents = if (mapping == AuditMapping.SNAKE) "component_types" else "componentTypes"
        val collectionSpec = legacy.get("collectionType", Document::class.java)
        val (target, suffix) = when (mutation) {
            LegacyMutation.SCALAR_COLLISION -> legacy.get(collisionKey, Document::class.java) to "/$collisionKey/$scalarName"
            LegacyMutation.COLLECTION_EXTRA_TYPE_NAME -> collectionSpec to "/collectionType/$scalarName"
            LegacyMutation.NULL_COMPONENT_EXTRA_TYPE_NAME -> {
                val components = mapped(collectionSpec, CollectionType::class.java, "componentTypes") as List<*>
                components[1] as Document to "/collectionType/$collectionComponents/1/$scalarName"
            }
            LegacyMutation.NESTED_EXTRA_TYPE_NAME -> {
                val nested = Document()
                collectionSpec["extra"] = nested
                nested to "/collectionType/extra/$scalarName"
            }
            LegacyMutation.LITERAL_DOT_KEY -> legacy.get("literal~key", Document::class.java) to "/literal~0key/$scalarName"
            LegacyMutation.SLASH_KEY -> legacy.get("slash/key", Document::class.java) to "/slash~1key/$scalarName"
        }
        val nameParts = mappingAccess.fieldNames.fieldParts(SimpleType::class.java, "typeName")
        var nameParent = target
        nameParts.dropLast(1).forEach { segment ->
            nameParent = nameParent.get(segment, Document::class.java) ?: Document().also { nameParent[segment] = it }
        }
        nameParent[nameParts.last()] = WITNESS_NAME
        collection(type).replaceOne(eq("_id", id), source)
        val before = inventorySnapshot()

        assertUnknownType(catchThrowable { read() }, TypeRole.VALUE, WITNESS_NAME)
        val rootPath = mapping.recordPaths[5 + position.ordinal].substringBefore("/payload/")
        assertFinding(type, id.toHexString(), rootPath + suffix, TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LegacyInnerNameProbe::class)
    fun `O03 legacy SimpleType names preserve optional and invalid value contracts`(probe: LegacyInnerNameProbe) {
        val values = if (probe == LegacyInnerNameProbe.APPLICATION_KEY) {
            mapOf("typeName" to "application value", "payload" to "safe value")
        } else {
            mapOf("payload" to "safe value")
        }
        val id = ObjectId()
        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val data = StepData(id.toHexString(), workflow.value, AUDIT_STEP_KIND, "1", values, Status.Active, emptyMap())
        mappingSteps.create(data)
        val type = StepDocument::class.java
        val read = { assertThat(mappingSteps.findForWorkflowAndId(workflow, StepIdentifier(data.id))).isEqualTo(data); Unit }
        read()
        val source = raw(type, Document("_id", id))
        val currentParts = mappingAccess.fieldNames.fieldParts(type, "dataEntries")
        val currentParent = currentParts.dropLast(1).fold(source) { parent, segment -> parent.get(segment, Document::class.java) }
        currentParent.remove(currentParts.last())
        collection(type).replaceOne(eq("_id", id), source)
        read()
        assertCleanInventory()

        if (probe != LegacyInnerNameProbe.APPLICATION_KEY) {
            val legacy = mapped(source, type, "dataTypeMap") as Document
            val simpleType = legacy.get("payload", Document::class.java)
            when (probe) {
                LegacyInnerNameProbe.MISSING -> removeMapped(simpleType, SimpleType::class.java, "typeName")
                LegacyInnerNameProbe.NULL -> setMapped(simpleType, SimpleType::class.java, "typeName", null)
                LegacyInnerNameProbe.NONSTRING -> setMapped(simpleType, SimpleType::class.java, "typeName", 42)
                LegacyInnerNameProbe.KNOWN_OTHER_ROLE -> setMapped(simpleType, SimpleType::class.java, "typeName", MODEL_TYPE_ALIAS)
                LegacyInnerNameProbe.APPLICATION_KEY -> error("handled above")
            }
            collection(type).replaceOne(eq("_id", id), source)
        }
        val before = inventorySnapshot()
        val legacyPath = mapping.recordPaths[5]
        when (probe) {
            LegacyInnerNameProbe.APPLICATION_KEY -> {
                read()
                assertCleanInventory()
            }
            LegacyInnerNameProbe.KNOWN_OTHER_ROLE -> {
                assertUnknownType(catchThrowable { read() }, TypeRole.VALUE, MODEL_TYPE_ALIAS)
                assertFinding(type, id.toHexString(), legacyPath, TypeRole.VALUE,
                    MongoTypeAuditIssue.DISALLOWED, MODEL_TYPE_ALIAS)
            }
            LegacyInnerNameProbe.MISSING -> {
                assertThat(catchThrowable { read() }).isInstanceOf(RuntimeException::class.java)
                assertCleanInventory()
            }
            LegacyInnerNameProbe.NULL, LegacyInnerNameProbe.NONSTRING -> {
                val failure = catchThrowable { read() }
                assertThat(failure).isInstanceOf(RuntimeException::class.java)
                assertFinding(type, id.toHexString(), legacyPath, TypeRole.VALUE,
                    MongoTypeAuditIssue.MALFORMED, name = null)
            }
        }
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RecordField::class)
    fun `O03 mapped record type names agree with the public persistence rejection`(field: RecordField) {
        val value = SecurityTestWorkflowValue("mapped audit")
        val id = ObjectId()
        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val (type, read) = when (field) {
            RecordField.STEP_DATA, RecordField.STEP_METADATA, RecordField.LEGACY_STEP_DATA, RecordField.LEGACY_STEP_METADATA -> {
                val data = StepData(id.toHexString(), workflow.value, AUDIT_STEP_KIND, "1",
                    mapOf("payload" to value), Status.Active, mapOf("payload" to value))
                mappingSteps.create(data)
                StepDocument::class.java to { assertThat(mappingSteps.findForWorkflowAndId(workflow, StepIdentifier(data.id))).isEqualTo(data); Unit }
            }
            RecordField.JOB_PARAMETERS, RecordField.LEGACY_JOB_PARAMETERS -> {
                val data = JobData(id.toHexString(), workflow.value, AUDIT_JOB_KIND,
                    mapOf("payload" to value), Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
                mappingJobs.create(data)
                JobDocument::class.java to { assertThat(mappingJobs.findForWorkflowAndId(workflow, JobIdentifier(data.id))).isEqualTo(data); Unit }
            }
            else -> {
                val data = StepDefinitionData(AUDIT_STEP_KIND, "1", mapOf("payload" to value),
                    listOf(DataDefinitionData("payload", String::class.java.name, mapOf("payload" to value), false)))
                mappingDefinitions.save(data)
                StepDefinitionDocument::class.java to { assertThat(mappingDefinitions.findForKindAndVersion(data.kind, data.version)).isEqualTo(data); Unit }
            }
        }
        val storedId = if (type == StepDefinitionDocument::class.java) raw(type, Document()).get("_id") else id
        val filter = eq("_id", storedId)
        read()
        assertCleanInventory()
        val source = raw(type, Document("_id", storedId))
        val record = when (field) {
            RecordField.DEFINITION_DATA_METADATA -> {
                val data = mapped(source, type, "data") as List<*>
                mapped(data.single() as Document, DataDefinitionDocument::class.java, "metadata") as Document
            }
            else -> mapped(source, type, field.property) as Document
        }
        if (field.legacy) {
            val spec = record.get("payload", Document::class.java)
            setMapped(spec, SimpleType::class.java, "typeName", WITNESS_NAME)
        } else {
            val jvm = mapped(record, TypedRecords::class.java, "jvmTypes") as Document
            val entries = mapped(jvm, JvmTypeMapping::class.java, "entries") as List<*>
            assertThat(entries).hasSize(1)
            setMapped(entries.single() as Document, TypeRecordEntry::class.java, "type", WITNESS_NAME)
        }
        collection(type).replaceOne(filter, source)
        val before = inventorySnapshot()

        assertUnknownType(catchThrowable { read() }, TypeRole.VALUE, WITNESS_NAME)
        assertFinding(type, storedId.toString(), mapping.recordPaths[field.ordinal], TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DirectEntryTypeProbe::class)
    fun `O03 direct JVM entry type classifications retain mapped paths and read parity`(probe: DirectEntryTypeProbe) {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val jvmTypes = mapped(fixture.records, TypedRecords::class.java, "jvmTypes") as Document
        val entry = (mapped(jvmTypes, JvmTypeMapping::class.java, "entries") as List<*>).single() as Document
        val persistedName: Any? = when (probe) {
            DirectEntryTypeProbe.UNKNOWN -> WITNESS_NAME
            DirectEntryTypeProbe.KNOWN_OTHER_ROLE -> MODEL_TYPE_ALIAS
            DirectEntryTypeProbe.MISSING -> null
            DirectEntryTypeProbe.NULL -> null
        }
        when (probe) {
            DirectEntryTypeProbe.MISSING -> removeMapped(entry, TypeRecordEntry::class.java, "type")
            else -> setMapped(entry, TypeRecordEntry::class.java, "type", persistedName)
        }
        collection(fixture.type).replaceOne(eq("_id", fixture.id), fixture.source)
        val before = inventorySnapshot()

        val readFailure = catchThrowable { fixture.read() }
        when (probe) {
            DirectEntryTypeProbe.UNKNOWN -> assertUnknownType(readFailure, TypeRole.VALUE, WITNESS_NAME)
            DirectEntryTypeProbe.KNOWN_OTHER_ROLE -> assertUnknownType(readFailure, TypeRole.VALUE, MODEL_TYPE_ALIAS)
            DirectEntryTypeProbe.MISSING, DirectEntryTypeProbe.NULL ->
                assertThat(readFailure).isInstanceOf(RuntimeException::class.java)
        }

        val expectedIssue = when (probe) {
            DirectEntryTypeProbe.UNKNOWN -> MongoTypeAuditIssue.UNREGISTERED
            DirectEntryTypeProbe.KNOWN_OTHER_ROLE -> MongoTypeAuditIssue.DISALLOWED
            DirectEntryTypeProbe.MISSING, DirectEntryTypeProbe.NULL -> MongoTypeAuditIssue.MALFORMED
        }
        val report = mappingAudit.audit()
        assertThat(report.complete).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(tuple(
                mappingAccess.template.getCollectionName(fixture.type),
                fixture.id.toString(),
                mapping.recordPaths[RecordField.STEP_DATA.ordinal],
                TypeRole.VALUE,
                expectedIssue,
                persistedName as? String,
            ))
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @Test
    fun `O03 direct JVM entry findings preserve entry order when multiple types are invalid`() {
        val fixture = currentRecordFixture(RecordField.STEP_DATA, mapOf("payload" to "safe value"))
        val jvmTypes = mapped(fixture.records, TypedRecords::class.java, "jvmTypes") as Document
        val entries = (mapped(jvmTypes, JvmTypeMapping::class.java, "entries") as List<*>).toMutableList()
        val first = entries.single() as Document
        setMapped(first, TypeRecordEntry::class.java, "type", WITNESS_NAME)
        val second = Document(first)
        setMapped(second, TypeRecordEntry::class.java, "reference", "additional-reference")
        setMapped(second, TypeRecordEntry::class.java, "type", MODEL_TYPE_ALIAS)
        entries.add(second)
        setMapped(jvmTypes, JvmTypeMapping::class.java, "entries", entries)
        collection(fixture.type).replaceOne(eq("_id", fixture.id), fixture.source)
        val before = inventorySnapshot()

        val readFailure = catchThrowable { fixture.read() }
        assertUnknownType(readFailure, TypeRole.VALUE, MODEL_TYPE_ALIAS)

        val report = mappingAudit.audit()
        val entriesPath = mapping.recordPaths[RecordField.STEP_DATA.ordinal]
            .substringBeforeLast('/')
            .substringBeforeLast('/')
        assertThat(report.complete).isTrue()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings)
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(
                tuple(mappingAccess.template.getCollectionName(fixture.type), fixture.id.toString(),
                    "$entriesPath/0/type", TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME),
                tuple(mappingAccess.template.getCollectionName(fixture.type), fixture.id.toString(),
                    "$entriesPath/1/type", TypeRole.VALUE, MongoTypeAuditIssue.DISALLOWED, MODEL_TYPE_ALIAS),
            )
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ModelTypeDeviation::class)
    fun `O03 mapped model type inventory remains independent from the trusted discriminator`(deviation: ModelTypeDeviation) {
        val id = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = securityTestModel()
        mappingWorkflows.create(model, id)
        assertThat(mappingWorkflows.find(id)?.model).isEqualTo(model)
        assertCleanInventory()
        val source = raw(WorkflowDocument::class.java, Document("_id", id.value))
        val persistedName = if (deviation == ModelTypeDeviation.INCONSISTENT) SecurityTestWorkflowSubtype::class.java.name else WITNESS_NAME
        setMapped(source, WorkflowDocument::class.java, "modelType", persistedName)
        collection(WorkflowDocument::class.java).replaceOne(eq("_id", id.value), source)
        val before = inventorySnapshot()

        assertThat(mappingWorkflows.find(id)?.model).isEqualTo(model)
        assertFinding(WorkflowDocument::class.java, id.value, mapping.modelTypePath, TypeRole.MODEL,
            if (deviation == ModelTypeDeviation.INCONSISTENT) MongoTypeAuditIssue.INCONSISTENT else MongoTypeAuditIssue.UNREGISTERED,
            persistedName)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @Test
    fun `O03 mapped workflow root discriminators reject unknown models in reads and audits`() {
        val id = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = securityTestModel()
        mappingWorkflows.create(model, id)
        assertThat(mappingWorkflows.find(id)?.model).isEqualTo(model)
        assertCleanInventory()
        val source = raw(WorkflowDocument::class.java, Document("_id", id.value))
        (mapped(source, WorkflowDocument::class.java, "model") as Document)[mapping.typeKey] = WITNESS_NAME
        collection(WorkflowDocument::class.java).replaceOne(eq("_id", id.value), source)
        val before = inventorySnapshot()

        assertUnknownType(catchThrowable { mappingWorkflows.find(id) }, TypeRole.MODEL, WITNESS_NAME)
        assertFinding(WorkflowDocument::class.java, id.value, "${mapping.modelPath}/${mapping.typeKey}", TypeRole.MODEL,
            MongoTypeAuditIssue.UNREGISTERED)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    @Test
    fun `O03 literal dotted keys retain their BSON pointer segment`() {
        assertNestedFinding(escaped = false)
    }

    @Test
    fun `O03 audit escapes slash and tilde in nested BSON pointer segments`() {
        assertNestedFinding(escaped = true)
    }

    private fun assertNestedFinding(escaped: Boolean) {
        val id = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = SecurityLiteralKeyModel(SecurityTestWorkflowValue("literal"))
        mappingWorkflows.create(model, id)
        assertThat(mappingWorkflows.find(id)?.model).isEqualTo(model)
        assertCleanInventory()
        val source = raw(WorkflowDocument::class.java, Document("_id", id.value))
        val root = mapped(source, WorkflowDocument::class.java, "model") as Document
        assertThat(root).containsKey("literal.payload").doesNotContainKey("literal")
        val payload = root.get("literal.payload", Document::class.java)
        val suffix = if (escaped) {
            payload["slash/~"] = Document(mapping.typeKey, WITNESS_NAME)
            "/literal.payload/slash~1~0/${mapping.typeKey}"
        } else {
            payload[mapping.typeKey] = WITNESS_NAME
            "/literal.payload/${mapping.typeKey}"
        }
        collection(WorkflowDocument::class.java).replaceOne(eq("_id", id.value), source)
        val before = inventorySnapshot()

        assertUnknownType(catchThrowable { mappingWorkflows.find(id) }, TypeRole.VALUE, WITNESS_NAME)
        assertFinding(WorkflowDocument::class.java, id.value, mapping.modelPath + suffix, TypeRole.VALUE,
            MongoTypeAuditIssue.UNREGISTERED)
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    private fun assertCleanInventory() {
        val before = inventorySnapshot()
        assertThat(mappingAudit.audit().isCompatible).isTrue()
        assertThat(inventorySnapshot()).isEqualTo(before)
        assertThat(mappingWitness.events).isEmpty()
    }

    private fun assertFinding(type: Class<*>, id: String, path: String, role: TypeRole?, issue: MongoTypeAuditIssue,
                              name: String? = WITNESS_NAME) {
        val report = mappingAudit.audit()
        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(mappingAccess.template.getCollectionName(type), id, path, role, issue, name)
    }

    private fun mapped(document: Document, type: Class<*>, property: String): Any? =
        mappingAccess.fieldNames.read(document, type, property)

    private fun setMapped(document: Document, type: Class<*>, property: String, value: Any?) {
        val parts = mappingAccess.fieldNames.fieldParts(type, property)
        val parent = parts.dropLast(1).fold(document) { current, segment -> current.get(segment, Document::class.java) }
        parent[parts.last()] = value
    }

    private fun removeMapped(document: Document, type: Class<*>, property: String) {
        val parts = mappingAccess.fieldNames.fieldParts(type, property)
        val parent = parts.dropLast(1).fold(document) { current, segment -> current.get(segment, Document::class.java) }
        parent.remove(parts.last())
    }

    private fun collection(type: Class<*>) = mappingAccess.template.getCollection(mappingAccess.template.getCollectionName(type))
    private fun raw(type: Class<*>, filter: Document) = requireNotNull(collection(type).find(filter).first())
    private fun inventorySnapshot(): Map<String, List<String>> = roots.associate {
        mappingAccess.template.getCollectionName(it) to collection(it).find().map { value -> value.toJson() }.sorted()
    }

    enum class RecordField(val property: String, val legacy: Boolean = false) {
        STEP_DATA("dataEntries"), STEP_METADATA("metadataEntries"), JOB_PARAMETERS("parameterEntries"),
        DEFINITION_METADATA("metadata"), DEFINITION_DATA_METADATA("data"),
        LEGACY_STEP_DATA("dataTypeMap", true), LEGACY_STEP_METADATA("metadataTypeMap", true),
        LEGACY_JOB_PARAMETERS("parameterTypeMap", true),
    }

    enum class ModelTypeDeviation { INCONSISTENT, UNREGISTERED }

    enum class ExtraTypeNameForm { UNKNOWN, WHITESPACE, EMPTY, NULL, NONSTRING, WRONG_ROLE, REGISTERED, BUILTIN, MISSING }

    enum class DirectEntryTypeProbe { UNKNOWN, KNOWN_OTHER_ROLE, MISSING, NULL }

    enum class LegacyPosition(val legacyProperty: String, val currentProperty: String) {
        STEP_DATA("dataTypeMap", "dataEntries"), STEP_METADATA("metadataTypeMap", "metadataEntries"),
        JOB_PARAMETERS("parameterTypeMap", "parameterEntries"),
    }

    enum class LegacyMutation {
        SCALAR_COLLISION, COLLECTION_EXTRA_TYPE_NAME, NULL_COMPONENT_EXTRA_TYPE_NAME,
        NESTED_EXTRA_TYPE_NAME, LITERAL_DOT_KEY, SLASH_KEY,
    }

    enum class LegacyInnerNameProbe { MISSING, NULL, NONSTRING, KNOWN_OTHER_ROLE, APPLICATION_KEY }

    enum class AuditMapping(val modelPath: String, val modelTypePath: String, val typeKey: String,
                            val recordPaths: List<String>) {
        DEFAULT("/model", "/modelType", "_class", listOf(
            "/dataEntries/jvmTypes/entries/0/type", "/metadataEntries/jvmTypes/entries/0/type", "/parameterEntries/jvmTypes/entries/0/type",
            "/metadata/jvmTypes/entries/0/type", "/data/0/metadata/jvmTypes/entries/0/type",
            "/dataTypeMap/payload/typeName", "/metadataTypeMap/payload/typeName", "/parameterTypeMap/payload/typeName")),
        SNAKE("/model", "/model_type", "_class", listOf(
            "/data_entries/jvm_types/entries/0/type", "/metadata_entries/jvm_types/entries/0/type", "/parameter_entries/jvm_types/entries/0/type",
            "/metadata/jvm_types/entries/0/type", "/data/0/metadata/jvm_types/entries/0/type",
            "/data_type_map/payload/type_name", "/metadata_type_map/payload/type_name", "/parameter_type_map/payload/type_name")),
        PATH("/payload/model", "/modelType", "_class", listOf(
            "/records/dataEntries/record/jvmTypes/entries/0/type", "/records/metadataEntries/record/jvmTypes/entries/0/type", "/parameterEntries/record/jvmTypes/entries/0/type",
            "/records/metadata/record/jvmTypes/entries/0/type", "/schema/data/0/definition/metadata/record/jvmTypes/entries/0/type",
            "/dataTypeMap/payload/typeName", "/metadataTypeMap/payload/typeName", "/parameterTypeMap/payload/typeName")),
        CUSTOM("/model", "/modelType", "@type", DEFAULT.recordPaths),
    }

    companion object {
        @JvmStatic
        fun legacyApplicationKeyCases(): List<Arguments> = LegacyPosition.entries.flatMap { position ->
            LegacyMutation.entries.map { mutation -> Arguments.of(position, mutation) }
        }

        private val roots = listOf(WorkflowDocument::class.java, StepDocument::class.java,
            JobDocument::class.java, StepDefinitionDocument::class.java)
    }
}
