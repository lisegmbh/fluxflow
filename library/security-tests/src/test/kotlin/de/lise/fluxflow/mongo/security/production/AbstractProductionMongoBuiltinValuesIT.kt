package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

abstract class AbstractProductionMongoBuiltinValuesIT {
    @Autowired private lateinit var steps: StepPersistence
    @Autowired private lateinit var jobs: JobPersistence
    @Autowired private lateinit var host: MongoTemplate

    @BeforeEach
    fun clearBuiltinRecords() {
        listOf(StepDocument::class.java, JobDocument::class.java).forEach { type ->
            host.getCollection(host.getCollectionName(type)).deleteMany(Document())
        }
    }

    @Test fun `duration round trips through scalar and typed collections`() =
        roundTrip(Duration.ofSeconds(42, 123_000_000))
    @Test fun `period round trips through scalar and typed collections`() = roundTrip(Period.of(1, 2, 3))
    @Test fun `region zone round trips through scalar and typed collections`() = roundTrip(ZoneId.of("Europe/Berlin"))
    @Test fun `offset zone round trips through scalar and typed collections`() = roundTrip(ZoneId.of("+02:00"))
    @Test fun `URI round trips through scalar and typed collections`() =
        roundTrip(URI.create("https://example.invalid/value?x=1#part"))
    @Test fun `German locale round trips through scalar and typed collections`() = roundTrip(Locale.GERMANY)
    @Test fun `root locale round trips through scalar and typed collections`() = roundTrip(Locale.ROOT)
    @Test fun `script locale round trips through scalar and typed collections`() =
        roundTrip(Locale.forLanguageTag("zh-Hans-CN"))
    @Test fun `locale extensions round trip through scalar and typed collections`() =
        roundTrip(Locale.forLanguageTag("en-US-u-ca-japanese-x-fluxflow"))
    @Test fun `legacy Japanese locale round trips through scalar and typed collections`() =
        roundTrip(Locale("ja", "JP", "JP"))
    @Test fun `locale variant round trips through scalar and typed collections`() =
        roundTrip(Locale("en", "US", "POSIX"))
    @Test fun `script only locale is rejected before create and save`() =
        assertRejectedBeforeWrite(Locale.Builder().setScript("Latn").build())
    @Test fun `extensions only locale is rejected before create and save`() =
        assertRejectedBeforeWrite(Locale.Builder().setUnicodeLocaleKeyword("ca", "japanese").build())
    @Test fun `script only locale nested in map and list is rejected before create and save`() =
        assertRejectedBeforeWrite(mapOf("items" to listOf(mapOf("locale" to Locale.Builder().setScript("Latn").build()))))

    private fun assertRejectedBeforeWrite(value: Any) {
        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val unsafe = mapOf("scalar" to value)
        val safe = mapOf("scalar" to "safe")
        val step = StepData(ObjectId().toHexString(), workflow.value, "locale-step", "1", unsafe, Status.Active, unsafe)
        assertThatThrownBy { steps.create(step) }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
        assertThat(steps.findForWorkflowAndId(workflow, StepIdentifier(step.id))).isNull()
        val validStep = step.copy(data = safe, metadata = safe)
        steps.create(validStep)
        assertThatThrownBy { steps.save(step) }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
        assertThat(steps.findForWorkflowAndId(workflow, StepIdentifier(step.id))).isEqualTo(validStep)

        val job = JobData(ObjectId().toHexString(), workflow.value, "locale-job", unsafe,
            Instant.parse("2026-10-12T08:00:00Z"), null, JobStatus.Scheduled)
        assertThatThrownBy { jobs.create(job) }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
        assertThat(jobs.findForWorkflowAndId(workflow, JobIdentifier(job.id))).isNull()
        val validJob = job.copy(parameters = safe)
        jobs.create(validJob)
        assertThatThrownBy { jobs.save(job) }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
        assertThat(jobs.findForWorkflowAndId(workflow, JobIdentifier(job.id))).isEqualTo(validJob)
    }

    private fun roundTrip(value: Any) {
        val values = linkedMapOf("scalar" to value, "collection" to listOf(value))
        val hostShape = Document()
        host.converter.write(BuiltinHostRecord("shape", values), hostShape)
        val hostScalar = hostShape.get("values", Document::class.java)["scalar"]
        assertThat(hostScalar).isInstanceOf(String::class.java)
        println("Builtin host BSON: ${value.javaClass.name}; value=$value; values=${hostShape["values"]}")

        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val step = StepData(ObjectId().toHexString(), workflow.value, "builtin-step", "1", values, Status.Active, values)
        steps.create(step)
        assertThat(steps.findForWorkflowAndId(workflow, StepIdentifier(step.id))).isEqualTo(step)
        val savedValues = values + ("marker" to "saved")
        val savedStep = step.copy(data = savedValues, metadata = savedValues)
        steps.save(savedStep)
        assertThat(steps.findForWorkflowAndId(workflow, StepIdentifier(step.id))).isEqualTo(savedStep)

        val job = JobData(ObjectId().toHexString(), workflow.value, "builtin-job", values,
            Instant.parse("2026-10-12T08:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(workflow, JobIdentifier(job.id))).isEqualTo(job)
        val savedJob = job.copy(parameters = savedValues)
        jobs.save(savedJob)
        assertThat(jobs.findForWorkflowAndId(workflow, JobIdentifier(job.id))).isEqualTo(savedJob)
    }

    data class BuiltinHostRecord(val id: String, val values: Map<String, Any>)
}
