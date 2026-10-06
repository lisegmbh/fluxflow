package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
import de.lise.fluxflow.mongo.security.fixtures.MODEL_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.assertUnknownType
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.bson.Document
import org.bson.types.ObjectId
import org.bson.types.Decimal128
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.UUID
import java.time.Instant
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URL

abstract class AbstractProductionMongoNumericIT {
    @Autowired private lateinit var steps: StepPersistence
    @Autowired private lateinit var host: MongoTemplate
    @Autowired private lateinit var jobs: JobPersistence

    @BeforeEach fun clearSteps() {
        collection().deleteMany(Document())
        host.getCollection(host.getCollectionName(BigIntegerHostDocument::class.java)).deleteMany(Document())
        host.getCollection(host.getCollectionName(JobDocument::class.java)).deleteMany(Document())
    }

    @Test fun `byte values and typed collections round trip through step data and metadata`() {
        val values = mapOf("scalar" to Byte.MAX_VALUE, "collection" to listOf(Byte.MIN_VALUE, Byte.MAX_VALUE))
        val step = newStep(values)
        steps.create(step)
        val raw = requireNotNull(collection().find(eq("_id", ObjectId(step.id))).first())
        val rawValues = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(rawValues["scalar"]).isInstanceOf(Int::class.javaObjectType).isEqualTo(127)
        assertThat(rawValues["collection"]).isEqualTo(listOf(-128, 127))
        assertThat(read(step)).isEqualTo(step)
    }

    private fun newStep(values: Map<String, Any>): StepData = StepData(ObjectId().toHexString(),
        UUID.randomUUID().toString(), "numeric-step", "1", values, Status.Active, values)

    private fun read(step: StepData) = steps.findForWorkflowAndId(WorkflowIdentifier(step.workflowId), StepIdentifier(step.id))
    private fun collection() = host.getCollection(host.getCollectionName(StepDocument::class.java))

    @Test fun `short values and typed collections round trip through step data and metadata`() {
        val step = newStep(mapOf("scalar" to Short.MAX_VALUE, "collection" to listOf(Short.MIN_VALUE, Short.MAX_VALUE)))
        steps.create(step)
        val raw = requireNotNull(collection().find(eq("_id", ObjectId(step.id))).first())
        val values = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(values["scalar"]).isInstanceOf(Int::class.javaObjectType).isEqualTo(32767)
        assertThat(values["collection"]).isEqualTo(listOf(-32768, 32767))
        assertThat(read(step)).isEqualTo(step)
    }

    @Test fun `byte values and collections round trip through job parameters`() {
        val data = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
            mapOf("scalar" to Byte.MIN_VALUE, "collection" to listOf(Byte.MIN_VALUE, Byte.MAX_VALUE)),
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id))).isEqualTo(data)
    }

    @Test fun `byte restoration rejects overflow and non integer BSON values in data and metadata`() {
        listOf(128, -129, 1.5, "127", 1L).forEach { invalid ->
            listOf("dataEntries", "metadataEntries").forEach { container ->
                val step = newStep(mapOf("scalar" to 1.toByte()))
                steps.create(step)
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                assertThatThrownBy { read(step) }.isInstanceOf(ValueTypeConversionException::class.java)
            }
        }
    }

    @Test fun `byte restoration never authorizes unknown or model only type keys`() {
        listOf("unknown.numeric.Type", MODEL_TYPE_ALIAS).forEach { alias ->
            val step = newStep(mapOf("scalar" to 1.toByte()))
            steps.create(step)
            collection().updateOne(eq("_id", ObjectId(step.id)), set("dataEntries.jvmTypes.entries.0.type", alias))
            assertUnknownType(catchThrowable { read(step) }, TypeRole.VALUE, alias)
        }
    }

    @Test fun `short values and collections round trip through job parameters`() {
        val data = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
            mapOf("scalar" to Short.MIN_VALUE, "collection" to listOf(Short.MIN_VALUE, Short.MAX_VALUE)),
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id))).isEqualTo(data)
    }

    @Test fun `short restoration rejects overflow and non integer BSON values in data and metadata`() {
        listOf(32768, -32769, 1.5, "32767", 1L).forEach { invalid ->
            listOf("dataEntries", "metadataEntries").forEach { container ->
                val step = newStep(mapOf("scalar" to 1.toShort()))
                steps.create(step)
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                assertThatThrownBy { read(step) }.isInstanceOf(ValueTypeConversionException::class.java)
            }
        }
    }

    @Test fun `float values and limits round trip through step data metadata and collections`() {
        val values = listOf(Float.MIN_VALUE, -Float.MIN_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE,
            0.0f, -0.0f, 0.1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val step = newStep(mapOf("scalar" to 1.25f, "collection" to values))
        steps.create(step)
        val raw = requireNotNull(collection().find(eq("_id", ObjectId(step.id))).first())
        val rawValues = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(rawValues["scalar"]).isInstanceOf(Double::class.javaObjectType).isEqualTo(1.25)
        assertThat(rawValues["collection"]).isEqualTo(values.map(Float::toDouble))
        val restored = requireNotNull(read(step))
        assertThat(restored).isEqualTo(step)
        listOf(restored.data, restored.metadata).forEach { records ->
            assertThat((records["collection"] as List<*>).map { (it as Float).toRawBits() })
                .isEqualTo(values.map(Float::toRawBits))
        }
    }

    @Test fun `float values and limits round trip through job parameters`() {
        val values = listOf(Float.MIN_VALUE, Float.MAX_VALUE, 0.0f, -0.0f, 0.1f, Float.NaN,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val data = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
            mapOf("scalar" to 1.25f, "collection" to values),
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        val restored = requireNotNull(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id)))
        assertThat(restored).isEqualTo(data)
        assertThat((restored.parameters["collection"] as List<*>).map { (it as Float).toRawBits() })
            .isEqualTo(values.map(Float::toRawBits))
    }

    @Test fun `float restoration rejects lossy doubles and incompatible BSON values`() {
        val invalidValues = listOf(0.1, Double.MAX_VALUE, Double.MIN_VALUE,
            Math.nextUp(Float.MAX_VALUE.toDouble()), 1, 1L, "1.25")
        invalidValues.forEach { invalid ->
            listOf("dataEntries", "metadataEntries").forEach { container ->
                val step = newStep(mapOf("scalar" to 1.25f))
                steps.create(step)
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                assertThatThrownBy { read(step) }.isInstanceOf(ValueTypeConversionException::class.java)
            }
            val job = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
                mapOf("scalar" to 1.25f), Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
            jobs.create(job)
            host.getCollection(host.getCollectionName(JobDocument::class.java))
                .updateOne(eq("_id", ObjectId(job.id)), set("parameterEntries.values.scalar", invalid))
            assertThatThrownBy {
                jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))
            }.isInstanceOf(ValueTypeConversionException::class.java)
        }
    }

    @Test fun `big decimal scale and collections round trip alongside native decimal128`() {
        val value = BigDecimal("12345678901234567890.123400")
        val step = newStep(mapOf("scalar" to value, "collection" to listOf(value, BigDecimal("0.0001000")),
            "native" to Decimal128(value)))
        steps.create(step)
        val raw = requireNotNull(collection().find(eq("_id", ObjectId(step.id))).first())
        val rawValues = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(rawValues["native"]).isInstanceOf(Decimal128::class.java)
        assertThat(rawValues["scalar"]).isInstanceOfAny(String::class.java, Decimal128::class.java)
        val restored = requireNotNull(read(step))
        assertThat(restored).isEqualTo(step)
        assertThat((restored.data["scalar"] as BigDecimal).scale()).isEqualTo(value.scale())
    }

    @Test fun `big decimal parameters and finite decimal128 values preserve scale`() {
        val value = BigDecimal("12345678901234567890.123400")
        val job = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
            mapOf("scalar" to value, "collection" to listOf(value, BigDecimal("0.0001000"))),
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))).isEqualTo(job)
        val step = newStep(mapOf("scalar" to value))
        steps.create(step)
        listOf(value.toString(), Decimal128(value)).forEach { representation ->
            listOf("dataEntries", "metadataEntries").forEach {
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$it.values.scalar", representation))
            }
            assertThat(read(step)).isEqualTo(step)
        }
    }

    @Test fun `big decimal restoration rejects malformed non finite and lossy numeric BSON values`() {
        listOf("not-decimal", "1E2147483648", Decimal128.NaN, Decimal128.POSITIVE_INFINITY,
            Decimal128.NEGATIVE_INFINITY, 1.2, 1L, true).forEach { invalid ->
            listOf("dataEntries", "metadataEntries").forEach { container ->
                val step = newStep(mapOf("scalar" to BigDecimal("1.2300")))
                steps.create(step)
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                assertThatThrownBy { read(step) }.isInstanceOf(ValueTypeConversionException::class.java)
            }
        }
    }

    @Test fun `big integer values beyond long range round trip through step data metadata and collections`() {
        val value = BigInteger("123456789012345678901234567890")
        val step = newStep(mapOf("scalar" to value, "collection" to listOf(value, value.negate())))
        steps.create(step)
        val raw = requireNotNull(collection().find(eq("_id", ObjectId(step.id))).first())
        val rawValues = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(rawValues["scalar"]).isInstanceOfAny(String::class.java, Decimal128::class.java)
        assertThat(read(step)).isEqualTo(step)
    }

    @Test fun `big integer job parameters and exact decimal128 values preserve all digits`() {
        val value = BigInteger("123456789012345678901234567890")
        val job = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
            mapOf("scalar" to value, "collection" to listOf(value, value.negate())),
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))).isEqualTo(job)
        val step = newStep(mapOf("scalar" to value))
        steps.create(step)
        listOf(value.toString(), Decimal128(BigDecimal(value))).forEach { representation ->
            listOf("dataEntries", "metadataEntries").forEach {
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$it.values.scalar", representation))
            }
            assertThat(read(step)).isEqualTo(step)
        }
    }

    @Test fun `big integer restoration rejects fractional malformed and incompatible BSON values`() {
        listOf("not-integer", "1.5", "1E3", Decimal128(BigDecimal("1.5")), Decimal128.NaN,
            Decimal128.POSITIVE_INFINITY, Decimal128.NEGATIVE_INFINITY, 1.0, 1L, true).forEach { invalid ->
            listOf("dataEntries", "metadataEntries").forEach { container ->
                val step = newStep(mapOf("scalar" to BigInteger.ONE))
                steps.create(step)
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                assertThatThrownBy { read(step) }.isInstanceOf(ValueTypeConversionException::class.java)
            }
            val job = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
                mapOf("scalar" to BigInteger.ONE), Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
            jobs.create(job)
            host.getCollection(host.getCollectionName(JobDocument::class.java))
                .updateOne(eq("_id", ObjectId(job.id)), set("parameterEntries.values.scalar", invalid))
            assertThatThrownBy {
                jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))
            }.isInstanceOf(ValueTypeConversionException::class.java)
        }
    }

    @Test fun `url values and typed collections preserve external form through step records`() {
        val value = URL("https://example.invalid/path?q=%2F#fragment")
        val values = listOf(value, URL("file:/tmp/fluxflow-value"))
        val step = newStep(mapOf("scalar" to value, "collection" to values))
        steps.create(step)
        val raw = requireNotNull(collection().find(eq("_id", ObjectId(step.id))).first())
        val rawValues = raw.get("dataEntries", Document::class.java).get("values", Document::class.java)
        assertThat(rawValues["scalar"]).isInstanceOf(String::class.java).isEqualTo(value.toExternalForm())
        val restored = requireNotNull(read(step))
        listOf(restored.data, restored.metadata).forEach { records ->
            assertThat((records["scalar"] as URL).toExternalForm()).isEqualTo(value.toExternalForm())
            assertThat((records["collection"] as List<*>).map { (it as URL).toExternalForm() })
                .isEqualTo(values.map(URL::toExternalForm))
        }
    }
    @Test fun `url job parameters preserve external form without network lookup`() {
        val value = URL("https://example.invalid/path?q=%2F#fragment")
        val values = listOf(value, URL("file:/tmp/fluxflow-value"))
        val job = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
            mapOf("scalar" to value, "collection" to values),
            Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(job)
        val restored = requireNotNull(jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id)))
        assertThat((restored.parameters["scalar"] as URL).toExternalForm()).isEqualTo(value.toExternalForm())
        assertThat((restored.parameters["collection"] as List<*>).map { (it as URL).toExternalForm() })
            .isEqualTo(values.map(URL::toExternalForm))
    }

    @Test fun `url restoration rejects malformed and incompatible BSON values`() {
        listOf("no-protocol", "http://[", "unsupported-scheme://example.invalid", 1, 1L, true).forEach { invalid ->
            listOf("dataEntries", "metadataEntries").forEach { container ->
                val step = newStep(mapOf("scalar" to URL("https://example.invalid/path")))
                steps.create(step)
                collection().updateOne(eq("_id", ObjectId(step.id)), set("$container.values.scalar", invalid))
                assertThatThrownBy { read(step) }.isInstanceOf(ValueTypeConversionException::class.java)
            }
            val job = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "numeric-job",
                mapOf("scalar" to URL("https://example.invalid/path")),
                Instant.parse("2026-10-06T12:00:00Z"), null, JobStatus.Scheduled)
            jobs.create(job)
            host.getCollection(host.getCollectionName(JobDocument::class.java))
                .updateOne(eq("_id", ObjectId(job.id)), set("parameterEntries.values.scalar", invalid))
            assertThatThrownBy {
                jobs.findForWorkflowAndId(WorkflowIdentifier(job.workflowId), JobIdentifier(job.id))
            }.isInstanceOf(ValueTypeConversionException::class.java)
        }
    }
    @Test fun `host big integer mapping has the same explicitly configured representation`() {
        val value = BigInteger("123456789012345678901234567890")
        val document = BigIntegerHostDocument(ObjectId().toHexString(), value, listOf(value, value.negate()))
        host.insert(document)
        val raw = requireNotNull(host.getCollection(host.getCollectionName(BigIntegerHostDocument::class.java))
            .find(eq("_id", ObjectId(document.id))).first())
        assertThat(raw["scalar"]).isInstanceOfAny(String::class.java, Decimal128::class.java)
        assertThat(raw["collection"]).isInstanceOf(List::class.java)
    }
}

@org.springframework.data.mongodb.core.mapping.Document("numeric_host_big_integer")
data class BigIntegerHostDocument(
    @org.springframework.data.annotation.Id val id: String,
    val scalar: BigInteger,
    val collection: List<BigInteger>,
)
