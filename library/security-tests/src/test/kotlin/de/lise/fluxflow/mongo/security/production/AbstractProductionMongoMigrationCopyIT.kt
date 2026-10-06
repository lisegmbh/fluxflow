package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.migration.MigrationError
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.bootstrapping.MigrateToTypeRecordsBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.PartialFailureAction
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowEnum
import de.lise.fluxflow.mongo.security.fixtures.VALUE_ENUM_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.securityTestEntry
import de.lise.fluxflow.mongo.security.fixtures.securityTestRegistry
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import java.time.Instant

abstract class AbstractProductionMongoMigrationCopyIT {
    @Autowired private lateinit var access: FluxFlowMongoAccess
    @Autowired private lateinit var steps: StepRepository
    @Autowired private lateinit var jobRepository: JobRepository
    @Autowired private lateinit var jobs: JobPersistence
    @Autowired private lateinit var stepPersistence: StepPersistence

    @BeforeEach fun clearCollections() {
        access.template.getCollection(access.template.getCollectionName(StepDocument::class.java)).deleteMany(Document())
        collection().deleteMany(Document())
    }

    @Test fun `migration rejects unreadable copied job records while migrating healthy neighbours`() {
        val before = legacyJob("healthy-before")
        val rejected = legacyJob("Ready", enumValue = true)
        val after = legacyJob("healthy-after")
        val original = raw(rejected)
        assertThat(read(rejected)?.parameters).containsEntry("value", SecurityTestWorkflowEnum.Ready)

        assertThatThrownBy { migration(PartialFailureAction.Fail, valueTypes()).setup() }
            .isInstanceOf(MigrationError::class.java).hasMessageContaining(rejected.id)

        assertThat(raw(rejected)).isEqualTo(original)
        listOf(before, after).forEach { healthy ->
            assertThat(raw(healthy)).containsKey("parameterEntries")
            assertThat(read(healthy)).isEqualTo(healthy)
        }
    }

    @Test fun `migration rejects unreadable copied step data and metadata while migrating healthy neighbours`() {
        val before = legacyStep("healthy-before")
        val dataRejected = legacyStep("Ready", setOf("data"))
        val metadataRejected = legacyStep("Ready", setOf("metadata"))
        val after = legacyStep("healthy-after")
        val originals = listOf(dataRejected, metadataRejected).associate { it.id to rawStep(it) }
        assertThat(readStep(dataRejected)?.data).containsEntry("value", SecurityTestWorkflowEnum.Ready)
        assertThat(readStep(metadataRejected)?.metadata).containsEntry("value", SecurityTestWorkflowEnum.Ready)

        assertThatThrownBy { migration(PartialFailureAction.Fail, valueTypes()).setup() }
            .isInstanceOf(MigrationError::class.java)
            .hasMessageContaining(dataRejected.id).hasMessageContaining(metadataRejected.id)

        listOf(dataRejected, metadataRejected).forEach { rejected ->
            assertThat(rawStep(rejected)).isEqualTo(originals[rejected.id])
        }
        listOf(before, after).forEach { healthy ->
            assertThat(rawStep(healthy)).containsKeys("dataEntries", "metadataEntries")
            assertThat(readStep(healthy)).isEqualTo(healthy)
        }
    }

    private fun legacyStep(value: String, enumFields: Set<String> = emptySet()): StepData {
        val step = StepData(ObjectId().toHexString(), "copy-validation-workflow", "copy-validation-step", "1",
            mapOf("value" to value), Status.Active, mapOf("value" to value))
        stepPersistence.create(step)
        stepCollection().updateOne(eq("_id", ObjectId(step.id)), unset("dataEntries"))
        stepCollection().updateOne(eq("_id", ObjectId(step.id)), unset("metadataEntries"))
        enumFields.forEach { field ->
            stepCollection().updateOne(eq("_id", ObjectId(step.id)), set("${field}TypeMap.value.typeName", VALUE_ENUM_ALIAS))
        }
        return step
    }

    private fun stepCollection() = access.template.getCollection(access.template.getCollectionName(StepDocument::class.java))
    private fun rawStep(step: StepData) = requireNotNull(stepCollection().find(eq("_id", ObjectId(step.id))).first())
    private fun readStep(step: StepData) = stepPersistence.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))

    @Test @ExtendWith(OutputCaptureExtension::class)
    fun `warn keeps rejected copied records intact and migrates healthy steps and jobs`(output: CapturedOutput) {
        val rejectedStep = legacyStep("Ready", setOf("data", "metadata"))
        val healthyStep = legacyStep("healthy-step")
        val rejectedJob = legacyJob("Ready", enumValue = true)
        val healthyJob = legacyJob("healthy-job")
        val originalStep = rawStep(rejectedStep)
        val originalJob = raw(rejectedJob)

        assertThatCode { migration(PartialFailureAction.Warn, valueTypes()).setup() }.doesNotThrowAnyException()

        assertThat(output.all).contains(rejectedStep.id, rejectedJob.id)
        assertThat(rawStep(rejectedStep)).isEqualTo(originalStep)
        assertThat(raw(rejectedJob)).isEqualTo(originalJob)
        assertThat(rawStep(healthyStep)).containsKeys("dataEntries", "metadataEntries")
        assertThat(raw(healthyJob)).containsKey("parameterEntries")
        assertThat(readStep(healthyStep)).isEqualTo(healthyStep)
        assertThat(read(healthyJob)).isEqualTo(healthyJob)
    }

    @Test fun `fail reports both collections after migrating healthy neighbours`() {
        val rejectedStep = legacyStep("Ready", setOf("metadata"))
        val healthyStep = legacyStep("healthy-step")
        val rejectedJob = legacyJob("Ready", enumValue = true)
        val healthyJob = legacyJob("healthy-job")
        val originalStep = rawStep(rejectedStep)
        val originalJob = raw(rejectedJob)

        assertThatThrownBy { migration(PartialFailureAction.Fail, valueTypes()).setup() }
            .isInstanceOf(MigrationError::class.java)
            .hasMessageContaining(rejectedStep.id).hasMessageContaining(rejectedJob.id)

        assertThat(rawStep(rejectedStep)).isEqualTo(originalStep)
        assertThat(raw(rejectedJob)).isEqualTo(originalJob)
        assertThat(rawStep(healthyStep)).containsKeys("dataEntries", "metadataEntries")
        assertThat(raw(healthyJob)).containsKey("parameterEntries")
        assertThat(readStep(healthyStep)).isEqualTo(healthyStep)
        assertThat(read(healthyJob)).isEqualTo(healthyJob)
    }

    @Test fun `logical and canonical enum registrations produce readable current records`() {
        val step = legacyStep("Ready", setOf("data", "metadata"))
        val job = legacyJob("Ready", enumValue = true)

        migration(PartialFailureAction.Fail, valueTypes(includeCanonical = true)).setup()

        assertThat(rawStep(step)).containsKeys("dataEntries", "metadataEntries")
        assertThat(raw(job)).containsKey("parameterEntries")
        assertThat(readStep(step)?.data).containsEntry("value", SecurityTestWorkflowEnum.Ready)
        assertThat(readStep(step)?.metadata).containsEntry("value", SecurityTestWorkflowEnum.Ready)
        assertThat(read(job)?.parameters).containsEntry("value", SecurityTestWorkflowEnum.Ready)
    }

    private fun valueTypes(includeCanonical: Boolean = false): ValueTypeConverter {
        val entries = mutableListOf(
            securityTestEntry(TypeRole.VALUE, VALUE_ENUM_ALIAS, SecurityTestWorkflowEnum::class.java.name),
        )
        if (includeCanonical) entries += securityTestEntry(TypeRole.VALUE,
            requireNotNull(SecurityTestWorkflowEnum::class.java.canonicalName), SecurityTestWorkflowEnum::class.java.name)
        return ValueTypeConverter(securityTestRegistry(WitnessClassLoader(), *entries.toTypedArray()))
    }

    private fun migration(failure: PartialFailureAction, converter: ValueTypeConverter) =
        MigrateToTypeRecordsBootstrapAction(failure, steps, jobRepository, access.converter, access.template, converter)

    private fun legacyJob(value: String, enumValue: Boolean = false): JobData {
        val job = JobData(ObjectId().toHexString(), "copy-validation-workflow", "copy-validation-job",
            mapOf("value" to value), Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        collection().updateOne(eq("_id", ObjectId(job.id)), unset("parameterEntries"))
        if (enumValue) collection().updateOne(eq("_id", ObjectId(job.id)), set("parameterTypeMap.value.typeName", VALUE_ENUM_ALIAS))
        return job
    }

    private fun collection() = access.template.getCollection(access.template.getCollectionName(JobDocument::class.java))
    private fun raw(job: JobData) = requireNotNull(collection().find(eq("_id", ObjectId(job.id))).first())
    private fun read(job: JobData) = jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))
}
