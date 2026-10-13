package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.security.fixtures.MODEL_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.assertUnknownType
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID

abstract class AbstractProductionMongoJavaTimeIT {
    @Autowired private lateinit var steps: StepPersistence
    @Autowired private lateinit var jobs: JobPersistence
    @Autowired private lateinit var host: MongoTemplate
    @org.springframework.beans.factory.annotation.Value("\${fluxflow.security.test.native-java-time:false}")
    private var nativeJavaTime: Boolean = false

    @BeforeEach fun clearCollections() {
        listOf(StepDocument::class.java, JobDocument::class.java, JavaTimeHostDocument::class.java).forEach {
            host.getCollection(host.getCollectionName(it)).deleteMany(Document())
        }
    }

    @Test fun `local date records follow actual host scalar mapping`() = roundTrip("date", LocalDate.of(2026, 10, 6))
    @Test fun `local time records follow actual host scalar mapping`() = roundTrip("time", LocalTime.of(10, 15, 30, 123_000_000))
    @Test fun `local date time records follow actual host scalar mapping`() =
        roundTrip("dateTime", LocalDateTime.of(2026, 10, 6, 10, 15, 30, 123_000_000))

    @Test fun `host date restoration rejects other sources and unknown or wrong role aliases`() {
        val values = listOf(LocalDate.of(2026, 10, 6), LocalTime.of(10, 15, 30),
            LocalDateTime.of(2026, 10, 6, 10, 15, 30))
        values.forEach { value ->
            listOf("2026-10-06", 1, 1L, true).forEach { invalid ->
                listOf("dataEntries", "metadataEntries").forEach { container ->
                    val records = mapOf("scalar" to value)
                    val step = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), "time-step", "1",
                        records, Status.Active, records)
                    steps.create(step)
                    host.getCollection(host.getCollectionName(StepDocument::class.java))
                        .updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                    assertThatThrownBy {
                        steps.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))
                    }.isInstanceOf(ValueTypeConversionException::class.java)
                }
            }
            listOf("unknown.time.Type", MODEL_TYPE_ALIAS).forEach { alias ->
                val records = mapOf("scalar" to value)
                val step = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), "time-step", "1",
                    records, Status.Active, records)
                steps.create(step)
                host.getCollection(host.getCollectionName(StepDocument::class.java))
                    .updateOne(eq("_id", ObjectId(step.id)), set("dataEntries.jvmTypes.entries.0.type", alias))
                assertUnknownType(catchThrowable {
                    steps.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))
                }, TypeRole.VALUE, alias)
            }
        }
    }

    @Test fun `legacy builtins only converter has no host date zone bridge`() {
        listOf(LocalDate::class.java, LocalTime::class.java, LocalDateTime::class.java).forEach { type ->
            assertThatThrownBy { SimpleType(type.name).assertType(Date(0)) }
                .isInstanceOf(ValueTypeConversionException::class.java)
        }
    }
    private fun roundTrip(field: String, value: Any) {
        assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of("Europe/Berlin"))
        val hostConverter = host.converter as org.springframework.data.mongodb.core.convert.MappingMongoConverter
        val originalTypeMapper = hostConverter.typeMapper
        val baseline = JavaTimeHostDocument(ObjectId().toHexString(), LocalDate.of(2026, 10, 6),
            LocalTime.of(10, 15, 30, 123_000_000), LocalDateTime.of(2026, 10, 6, 10, 15, 30, 123_000_000))
        host.insert(baseline)
        val baselineRaw = requireNotNull(host.getCollection(host.getCollectionName(JavaTimeHostDocument::class.java))
            .find(eq("_id", ObjectId(baseline.id))).first())
        assertThat(baselineRaw[field]).isInstanceOf(Date::class.java)
        val zone = if (nativeJavaTime) ZoneOffset.UTC else ZoneId.systemDefault()
        val expectedInstant = when (value) {
            is LocalDate -> value.atStartOfDay(zone).toInstant()
            is LocalDateTime -> value.atZone(zone).toInstant()
            is LocalTime -> value.atDate(if (nativeJavaTime) LocalDate.ofEpochDay(0) else LocalDate.now())
                .atZone(zone).toInstant()
            else -> error("Unsupported test value")
        }
        assertThat(baselineRaw[field]).isEqualTo(Date.from(expectedInstant))
        assertThat(host.findById(baseline.id, JavaTimeHostDocument::class.java)).isEqualTo(baseline)
        val values = mapOf("scalar" to value, "collection" to listOf(value, value))
        val step = StepData(ObjectId().toHexString(), UUID.randomUUID().toString(), "time-step", "1", values,
            Status.Active, values)
        steps.create(step)
        val raw = requireNotNull(host.getCollection(host.getCollectionName(StepDocument::class.java))
            .find(eq("_id", ObjectId(step.id))).first())
        val rawValues = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(rawValues["scalar"]).isInstanceOf(Date::class.java).isEqualTo(baselineRaw[field])
        assertThat(rawValues["collection"]).isEqualTo(listOf(baselineRaw[field], baselineRaw[field]))
        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))).isEqualTo(step)
        val job = JobData(ObjectId().toHexString(), step.workflowId, "time-job", values,
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))).isEqualTo(job)
        assertThat(hostConverter.typeMapper).isSameAs(originalTypeMapper)
    }
}

@org.springframework.data.mongodb.core.mapping.Document("java_time_host_baseline")
data class JavaTimeHostDocument(
    @org.springframework.data.annotation.Id val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val dateTime: LocalDateTime,
)