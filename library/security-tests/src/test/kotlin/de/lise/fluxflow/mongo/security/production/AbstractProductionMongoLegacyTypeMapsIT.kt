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
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.fixtures.AUDIT_STEP_KIND
import de.lise.fluxflow.mongo.security.fixtures.assertUnknownType
import de.lise.fluxflow.mongo.security.fixtures.AUDIT_JOB_KIND
import de.lise.fluxflow.mongo.security.production.AbstractProductionMongoAuditMappingContract.LegacyPosition
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.mongo.MongoFieldNames
import de.lise.fluxflow.mongo.bootstrapping.MigrateDataTypesMapBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.MigrateMetadataTypesMapBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.MigrateJobParameterTypesMapBootstrapAction
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import java.time.Instant
import java.util.UUID

abstract class AbstractProductionMongoLegacyTypeMapsIT(private val expectedTypeNamePath: String) {
    @Autowired private lateinit var access: FluxFlowMongoAccess
    @Autowired private lateinit var steps: StepPersistence
    @Autowired private lateinit var jobs: JobPersistence
    @Autowired private lateinit var audit: FluxFlowMongoTypeAudit
    @Autowired @Qualifier("productionWitnessClassLoader") private lateinit var witness: WitnessClassLoader
    private val fields get() = MongoFieldNames(access.converter.mappingContext)

    @BeforeEach fun clear() {
        listOf(StepDocument::class.java, JobDocument::class.java).forEach { collection(it).deleteMany(Document()) }
    }

    @Test
    fun `O03 current record extra type names honor multi segment infrastructure field mappings`() {
        val pathMapping = expectedTypeNamePath == "/types/dataTypeMap/payload/jvm/name"
        val key = if (pathMapping) "typeName" else "type_name"
        val data = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), AUDIT_STEP_KIND, "1",
            mapOf(key to "current value"), Status.Active, emptyMap())
        steps.create(data)
        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), StepIdentifier(data.id))).isEqualTo(data)
        val valid = raw(StepDocument::class.java, data.id)
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(raw(StepDocument::class.java, data.id)).isEqualTo(valid)
        val source = raw(StepDocument::class.java, data.id)
        val records = fields.read(source, StepDocument::class.java, "dataEntries") as Document
        val types = fields.read(records, TypedRecords::class.java, "types") as Document
        var target = types.get(key, Document::class.java)
        val parts = fields.fieldParts(SimpleType::class.java, "typeName")
        parts.dropLast(1).forEach { segment ->
            target = target.get(segment, Document::class.java) ?: Document().also { target[segment] = it }
        }
        target[parts.last()] = WITNESS_NAME
        replace(StepDocument::class.java, data.id, source)
        val before = raw(StepDocument::class.java, data.id)

        assertUnknownType(catchThrowable {
            steps.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), StepIdentifier(data.id))
        }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(access.template.getCollectionName(StepDocument::class.java), data.id,
                if (pathMapping) "/records/dataEntries/types/typeName/jvm/name" else "/data_entries/types/type_name/type_name",
                TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(raw(StepDocument::class.java, data.id)).isEqualTo(before)
        assertThat(witness.events).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LegacyPosition::class)
    fun `O03 legacy application keys keep mapped inner type name checks`(position: LegacyPosition) {
        val pathMapping = expectedTypeNamePath == "/types/dataTypeMap/payload/jvm/name"
        val key = if (pathMapping) "typeName" else "type_name"
        val values: Map<String, Any?> = linkedMapOf("typeName" to "dynamic", "type_name" to "mapped dynamic",
            "collectionType" to listOf("one", null, listOf("two")), "componentTypes" to "application value",
            "nullable" to null, "literal.key" to "literal", "slash/key" to "slash")
        val id = ObjectId().toHexString()
        val workflowId = WorkflowIdentifier(UUID.randomUUID().toString())
        val (type, read) = if (position == LegacyPosition.JOB_PARAMETERS) {
            val data = JobData(id, workflowId.value, AUDIT_JOB_KIND, values,
                Instant.parse("2026-10-05T12:00:00Z"), null, JobStatus.Scheduled)
            jobs.create(data)
            JobDocument::class.java to { assertThat(jobs.findForWorkflowAndId(workflowId, JobIdentifier(id))).isEqualTo(data); Unit }
        } else {
            val data = StepData(id, workflowId.value, AUDIT_STEP_KIND, "1",
                if (position == LegacyPosition.STEP_DATA) values else emptyMap(), Status.Active,
                if (position == LegacyPosition.STEP_METADATA) values.filterValues { it != null }.mapValues { requireNotNull(it.value) } else emptyMap())
            steps.create(data)
            StepDocument::class.java to { assertThat(steps.findForWorkflowAndId(workflowId, StepIdentifier(id))).isEqualTo(data); Unit }
        }
        read()
        val source = raw(type, id)
        remove(source, type, position.currentProperty)
        replace(type, id, source)
        read()
        val valid = raw(type, id)
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(raw(type, id)).isEqualTo(valid)
        assertThat(witness.events).isEmpty()
        val spec = (fields.read(source, type, position.legacyProperty) as Document).get(key, Document::class.java)
        val parts = fields.fieldParts(SimpleType::class.java, "typeName")
        val parent = parts.dropLast(1).fold(spec) { current, part -> current.get(part, Document::class.java) }
        parent[parts.last()] = WITNESS_NAME
        replace(type, id, source)
        val corrupted = raw(type, id)

        assertUnknownType(catchThrowable { read() }, TypeRole.VALUE, WITNESS_NAME)
        val expected = when (position) {
            LegacyPosition.STEP_DATA -> if (pathMapping) "/types/dataTypeMap/typeName/jvm/name" else "/data_type_map/type_name/type_name"
            LegacyPosition.STEP_METADATA -> if (pathMapping) "/types/metadataTypeMap/typeName/jvm/name" else "/metadata_type_map/type_name/type_name"
            LegacyPosition.JOB_PARAMETERS -> if (pathMapping) "/types/parameterTypeMap/typeName/jvm/name" else "/parameter_type_map/type_name/type_name"
        }
        val report = audit.audit()
        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(access.template.getCollectionName(type), id, expected,
                TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(raw(type, id)).isEqualTo(corrupted)
        assertThat(witness.events).isEmpty()
    }

    @Test
    fun `O03 audit follows mapped legacy type names including nested field paths`() {
        val data = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), AUDIT_STEP_KIND, "1",
            mapOf("payload" to "safe legacy value"), Status.Active, emptyMap())
        steps.create(data)
        val type = StepDocument::class.java
        val source = raw(type, data.id)
        remove(source, type, "dataEntries")
        replace(type, data.id, source)
        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), StepIdentifier(data.id)))
            .isEqualTo(data)
        val valid = raw(type, data.id)
        assertThat(audit.audit().isCompatible).isTrue()
        assertThat(raw(type, data.id)).isEqualTo(valid)
        assertThat(witness.events).isEmpty()

        val spec = (fields.read(source, type, "dataTypeMap") as Document).get("payload", Document::class.java)
        val parts = fields.fieldParts(SimpleType::class.java, "typeName")
        val parent = parts.dropLast(1).fold(spec) { current, part -> current.get(part, Document::class.java) }
        parent[parts.last()] = WITNESS_NAME
        replace(type, data.id, source)
        val corrupted = raw(type, data.id)

        assertUnknownType(catchThrowable {
            steps.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), StepIdentifier(data.id))
        }, TypeRole.VALUE, WITNESS_NAME)
        val report = audit.audit()

        assertThat(report.complete).isTrue()
        assertThat(report.isCompatible).isFalse()
        assertThat(report.failures).isEmpty()
        assertThat(report.findings).singleElement()
            .extracting("collection", "documentId", "path", "expectedRole", "issue", "persistedName")
            .containsExactly(access.template.getCollectionName(type), data.id, expectedTypeNamePath,
                TypeRole.VALUE, MongoTypeAuditIssue.UNREGISTERED, WITNESS_NAME)
        assertThat(raw(type, data.id)).isEqualTo(corrupted)
        assertThat(witness.events).isEmpty()
    }

    @Test
    fun `legacy step data and metadata migrations honor mapped output names and custom type key`() {
        val data = stepData()
        steps.create(data)
        val type = StepDocument::class.java
        val source = raw(type, data.id)
        legacy(source, type, "data", "dataTypes", "dataTypeMap", "dataEntries")
        legacy(source, type, "metadata", "metadataTypes", "metadataTypeMap", "metadataEntries")
        replace(type, data.id, source)

        MigrateDataTypesMapBootstrapAction(access.template).setup()
        MigrateMetadataTypesMapBootstrapAction(access.template).setup()

        val migrated = raw(type, data.id)
        assertSpecs(migrated, type, "dataTypeMap", "dataTypes")
        assertSpecs(migrated, type, "metadataTypeMap", "metadataTypes")
        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), StepIdentifier(data.id)))
            .isEqualTo(data)
        MigrateDataTypesMapBootstrapAction(access.template).setup()
        MigrateMetadataTypesMapBootstrapAction(access.template).setup()
        assertThat(raw(type, data.id)).isEqualTo(migrated)
    }

    @Test
    fun `legacy job parameter migration honors mapped output names and custom type key`() {
        val data = jobData()
        jobs.create(data)
        val type = JobDocument::class.java
        val source = raw(type, data.id)
        legacy(source, type, "parameters", "parameterTypes", "parameterTypeMap", "parameterEntries")
        replace(type, data.id, source)

        MigrateJobParameterTypesMapBootstrapAction(access.template).setup()

        val migrated = raw(type, data.id)
        assertSpecs(migrated, type, "parameterTypeMap", "parameterTypes")
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id)))
            .isEqualTo(data)
        MigrateJobParameterTypesMapBootstrapAction(access.template).setup()
        assertThat(raw(type, data.id)).isEqualTo(migrated)
    }

    @Test
    fun `legacy migrations do not rewrite documents with current mapped record containers`() {
        val step = stepData()
        val job = jobData()
        steps.create(step)
        jobs.create(job)
        val stepBefore = raw(StepDocument::class.java, step.id)
        val jobBefore = raw(JobDocument::class.java, job.id)
        remove(stepBefore, StepDocument::class.java, "dataTypeMap")
        remove(stepBefore, StepDocument::class.java, "metadataTypeMap")
        remove(jobBefore, JobDocument::class.java, "parameterTypeMap")
        replace(StepDocument::class.java, step.id, stepBefore)
        replace(JobDocument::class.java, job.id, jobBefore)

        MigrateDataTypesMapBootstrapAction(access.template).setup()
        MigrateMetadataTypesMapBootstrapAction(access.template).setup()
        MigrateJobParameterTypesMapBootstrapAction(access.template).setup()

        assertThat(raw(StepDocument::class.java, step.id)).isEqualTo(stepBefore)
        assertThat(raw(JobDocument::class.java, job.id)).isEqualTo(jobBefore)
        // This skip-only BSON deliberately lacks deprecated constructor-required maps.
        // Typed round trips are verified by the valid legacy-conversion contracts above.
    }

    private fun legacy(source: Document, type: Class<*>, valuesProperty: String, historicProperty: String,
                       mapProperty: String, recordsProperty: String) {
        val values = fields.read(source, type, valuesProperty) as Document
        // These raw historic names intentionally have no corresponding current mapped property.
        source[historicProperty] = Document(values.mapValues { (_, value) ->
            if (value == null) null else String::class.java.name
        })
        remove(source, type, mapProperty)
        remove(source, type, recordsProperty)
    }

    private fun assertSpecs(source: Document, type: Class<*>, mapProperty: String, historicProperty: String) {
        val migrated = fields.read(source, type, mapProperty) as? Document
        assertThat(migrated).describedAs("Migration must write the current mapped type map").isNotNull()
        val historical = source.get(historicProperty, Document::class.java)
        assertThat(migrated!!.keys).containsExactlyInAnyOrderElementsOf(historical.keys)
        historical.forEach { (key, name) ->
            val spec = migrated.get(key, Document::class.java)
            assertThat(spec.containsKey("_class")).isFalse()
            assertThat(spec.getString("@type"))
                .isEqualTo(if (name == null) NullType::class.java.name else SimpleType::class.java.name)
            if (name != null) assertThat(fields.read(spec, SimpleType::class.java, "typeName")).isEqualTo(name)
        }
    }

    private fun remove(source: Document, type: Class<*>, property: String) {
        val parts = fields.fieldParts(type, property)
        var parent = source
        parts.dropLast(1).forEach { part -> parent = parent.get(part, Document::class.java) ?: return }
        parent.remove(parts.last())
    }

    private fun collection(type: Class<*>) = access.template.getCollection(access.template.getCollectionName(type))
    private fun raw(type: Class<*>, id: String) = requireNotNull(collection(type).find(eq("_id", ObjectId(id))).first())
    private fun replace(type: Class<*>, id: String, source: Document) = collection(type).replaceOne(eq("_id", ObjectId(id)), source)
    private fun values(): Map<String, Any?> = linkedMapOf("payload" to "safe", "nullable" to null,
        "typeName" to "dynamic key", "literal.key" to "literal key")
    private fun stepData() = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), "legacy-types", "1",
        values(), Status.Active, values().filterValues { it != null }.mapValues { it.value!! })
    private fun jobData() = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "legacy-types", values(),
        Instant.parse("2026-10-05T12:00:00Z"), null, JobStatus.Scheduled)
}

@TestConfiguration
open class LegacyTypeMapPathConfiguration {
    companion object {
        @Bean @JvmStatic fun legacyTypeMapPaths(): BeanPostProcessor = object : BeanPostProcessor {
            override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
                if (bean is MongoMappingContext) bean.setFieldNamingStrategy { property ->
                    when {
                        property.owner.type == SimpleType::class.java && property.name == "typeName" -> "jvm.name"
                        property.name in setOf("dataTypeMap", "metadataTypeMap", "parameterTypeMap") -> "types.${property.name}"
                        property.name in setOf("dataEntries", "metadataEntries", "parameterEntries") -> "records.${property.name}"
                        else -> property.name
                    }
                }
                return bean
            }
        }
    }
}
