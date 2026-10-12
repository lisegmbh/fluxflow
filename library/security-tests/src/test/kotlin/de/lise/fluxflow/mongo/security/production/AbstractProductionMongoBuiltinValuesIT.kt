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
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.bson.types.ObjectId
import org.bson.types.Binary
import org.bson.types.Decimal128
import org.bson.codecs.configuration.CodecConfigurationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mapping.MappingException
import org.springframework.beans.BeanInstantiationException
import java.lang.reflect.InaccessibleObjectException
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.Year
import java.time.YearMonth
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import java.util.Date

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
    @Test fun `year is not an automatic builtin before create and save`() = assertUnsupportedBuiltin(Year.of(2026))
    @Test fun `year month is not an automatic builtin before create and save`() = assertUnsupportedBuiltin(YearMonth.of(2026, 10))
    @Test fun `offset date time is not an automatic builtin before create and save`() =
        assertUnsupportedBuiltin(OffsetDateTime.parse("2026-10-12T10:15:30+02:00"))
    @Test fun `offset time is not an automatic builtin before create and save`() =
        assertUnsupportedBuiltin(OffsetTime.parse("10:15:30+02:00"))
    @Test fun `zoned date time is not an automatic builtin before create and save`() =
        assertUnsupportedBuiltin(ZonedDateTime.parse("2026-10-12T10:15:30+02:00[Europe/Berlin]"))
    @Test fun `unit is not an automatic builtin before create and save`() = assertUnsupportedBuiltin(Unit)

    @Test fun `boolean round trips through scalar and typed collections`() = roundTrip(true, null)
    @Test fun `long round trips through scalar and typed collections`() = roundTrip(Long.MAX_VALUE, null)
    @Test fun `double round trips through scalar and typed collections`() = roundTrip(1.5, null)
    @Test fun `character round trips through scalar and typed collections`() = roundTrip('é', null)
    @Test fun `date round trips through scalar and typed collections`() =
        roundTrip(Date.from(Instant.parse("2026-10-12T08:15:30.123Z")), null)
    @Test fun `UUID round trips through scalar and typed collections`() =
        roundTrip(UUID.fromString("12345678-1234-5678-9abc-123456789abc"), null)
    @Test fun `binary round trips through scalar and typed collections`() =
        roundTrip(Binary(byteArrayOf(1, 2, 3)), null)
    @Test fun `object id round trips through scalar and typed collections`() =
        roundTrip(ObjectId("507f1f77bcf86cd799439011"), null)
    @Test fun `native decimal128 round trips through scalar and typed collections`() =
        roundTrip(Decimal128.parse("1234.500"), null)
    @Test fun `BSON document round trips through scalar and typed collections`() =
        roundTrip(Document("inner", "safe"), null)

    private fun assertUnsupportedBuiltin(value: Any) {
        recordHostWriterEvidence(value)
        assertRejectedBeforeWrite(value, UnknownTypeException::class.java)
    }

    private fun recordHostWriterEvidence(value: Any) {
        val source = BuiltinHostRecord("shape", mapOf("scalar" to value))
        val raw = Document()
        try {
            host.converter.write(source, raw)
            val scalar = raw.get("values", Document::class.java)["scalar"]
            println("Unsupported candidate host BSON: ${value.javaClass.name}; scalarClass=${scalar?.javaClass?.name}; value=$scalar")
            raw.toBsonDocument(Document::class.java, host.getCollection(host.getCollectionName(StepDocument::class.java)).codecRegistry).toJson()
            val restored = host.converter.read(BuiltinHostRecord::class.java, raw).values["scalar"]
            println("Unsupported candidate host read: ${value.javaClass.name}; restoredClass=${restored?.javaClass?.name}; equal=${value == restored}")
        } catch (failure: InaccessibleObjectException) {
            println("Unsupported candidate host failure: ${value.javaClass.name}; ${failure.javaClass.name}: ${failure.message}")
        } catch (failure: MappingException) {
            println("Unsupported candidate host failure: ${value.javaClass.name}; ${failure.javaClass.name}: ${failure.message}")
        } catch (failure: BeanInstantiationException) {
            println("Unsupported candidate host failure: ${value.javaClass.name}; ${failure.javaClass.name}: ${failure.message}")
        } catch (failure: CodecConfigurationException) {
            println("Unsupported candidate host failure: ${value.javaClass.name}; ${failure.javaClass.name}: ${failure.message}")
        }
    }

    private fun assertRejectedBeforeWrite(value: Any, failureType: Class<out Throwable> = ValueTypeConversionException::class.java) {
        val workflow = WorkflowIdentifier(UUID.randomUUID().toString())
        val unsafe = mapOf("scalar" to value)
        val safe = mapOf("scalar" to "safe")
        val step = StepData(ObjectId().toHexString(), workflow.value, "locale-step", "1", unsafe, Status.Active, unsafe)
        assertThatThrownBy { steps.create(step) }.isExactlyInstanceOf(failureType)
        assertThat(steps.findForWorkflowAndId(workflow, StepIdentifier(step.id))).isNull()
        val validStep = step.copy(data = safe, metadata = safe)
        steps.create(validStep)
        assertThatThrownBy { steps.save(step) }.isExactlyInstanceOf(failureType)
        assertThat(steps.findForWorkflowAndId(workflow, StepIdentifier(step.id))).isEqualTo(validStep)

        val job = JobData(ObjectId().toHexString(), workflow.value, "locale-job", unsafe,
            Instant.parse("2026-10-12T08:00:00Z"), null, JobStatus.Scheduled)
        assertThatThrownBy { jobs.create(job) }.isExactlyInstanceOf(failureType)
        assertThat(jobs.findForWorkflowAndId(workflow, JobIdentifier(job.id))).isNull()
        val validJob = job.copy(parameters = safe)
        jobs.create(validJob)
        assertThatThrownBy { jobs.save(job) }.isExactlyInstanceOf(failureType)
        assertThat(jobs.findForWorkflowAndId(workflow, JobIdentifier(job.id))).isEqualTo(validJob)
    }

    private fun roundTrip(value: Any, expectedHostType: Class<*>? = String::class.java) {
        val values = linkedMapOf("scalar" to value, "collection" to listOf(value))
        val hostShape = Document()
        host.converter.write(BuiltinHostRecord("shape", values), hostShape)
        val hostScalar = hostShape.get("values", Document::class.java)["scalar"]
        if (expectedHostType != null) assertThat(hostScalar).isInstanceOf(expectedHostType)
        println("Builtin host BSON: ${value.javaClass.name}; scalarClass=${hostScalar?.javaClass?.name}; value=$value; values=${hostShape["values"]}")

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
