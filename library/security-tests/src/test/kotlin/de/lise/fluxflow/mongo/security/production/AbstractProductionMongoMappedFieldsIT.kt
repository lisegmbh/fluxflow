package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.migration.MigrationError
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowValue
import de.lise.fluxflow.mongo.security.fixtures.MODEL_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.assertUnknownType
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.bootstrapping.MigrateToTypeRecordsBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.PartialFailureAction
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.DataDefinitionData
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.convert.ReadingConverter
import org.bson.Document
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.unset
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import java.time.Instant
import java.util.UUID

abstract class AbstractProductionMongoMappedFieldsIT {
    @Autowired
    private lateinit var steps: StepPersistence

    @Autowired
    private lateinit var jobs: JobPersistence

    @Autowired
    private lateinit var references: ScheduledJobReferencePersistence

    @Autowired
    private lateinit var definitions: StepDefinitionPersistence

    @Autowired
    private lateinit var hostTemplate: MongoTemplate

    @Autowired
    @Qualifier("productionWitnessClassLoader")
    private lateinit var witnessLoader: WitnessClassLoader

    @Autowired
    @Qualifier("migrateToTypeRecordsBootstrapAction")
    private lateinit var recordMigration: BootstrapAction

    @Autowired
    @Qualifier("mappedWarnMigration")
    private lateinit var warnMigration: BootstrapAction

    @BeforeEach
    fun clearDocuments() {
        listOf(StepDocument::class.java, JobDocument::class.java, StepDefinitionDocument::class.java)
            .forEach { hostTemplate.getCollection(hostTemplate.getCollectionName(it)).deleteMany(Document()) }
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `registered step data and metadata round trip with mapped record fields`() {
        val id = ObjectId().toHexString()
        val workflowId = UUID.randomUUID().toString()
        val data = StepData(id, workflowId, "mapped-step", "1",
            mapOf("payload" to SecurityTestWorkflowValue("data")), Status.Active,
            mapOf("payload" to SecurityTestWorkflowValue("metadata")))

        steps.create(data)

        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(workflowId), StepIdentifier(id)))
            .isEqualTo(data)

        hostTemplate.getCollection(hostTemplate.getCollectionName(StepDocument::class.java))
            .updateOne(eq("_id", ObjectId(id)), combine(unset("data_entries"), unset("metadata_entries")))
        assertThat(steps.findForWorkflowAndId(WorkflowIdentifier(workflowId), StepIdentifier(id)))
            .isEqualTo(data)
    }

    @Test
    fun `registered job parameters round trip through current and legacy mapped fields`() {
        val data = JobData(ObjectId().toHexString(), UUID.randomUUID().toString(), "mapped-job",
            mapOf("payload" to SecurityTestWorkflowValue("parameters")),
            Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        jobs.create(data)
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id)))
            .isEqualTo(data)

        hostTemplate.getCollection(hostTemplate.getCollectionName(JobDocument::class.java))
            .updateOne(eq("_id", ObjectId(data.id)), unset("parameter_entries"))
        assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(data.workflowId), JobIdentifier(data.id)))
            .isEqualTo(data)
    }

    @Test
    fun `definition and data definition metadata round trip with mapped record fields`() {
        val data = StepDefinitionData("mapped-definition-${UUID.randomUUID()}", "1",
            mapOf("payload" to SecurityTestWorkflowValue("definition")),
            listOf(DataDefinitionData("payload", String::class.java.name,
                mapOf("payload" to SecurityTestWorkflowValue("data-definition")), false)))
        definitions.save(data)

        assertThat(definitions.findForKindAndVersion(data.kind, data.version)).isEqualTo(data)
    }

    @Test
    fun `mapped value fields still reject model role aliases before materialization`() {
        val id = ObjectId().toHexString()
        val workflowId = UUID.randomUUID().toString()
        steps.create(StepData(id, workflowId, "mapped-step", "1",
            mapOf("payload" to SecurityTestWorkflowValue("data")), Status.Active, emptyMap()))
        hostTemplate.getCollection(hostTemplate.getCollectionName(StepDocument::class.java))
            .updateOne(eq("_id", ObjectId(id)), set("data_entries.values.payload", Document("_class", MODEL_TYPE_ALIAS)))

        val failure = org.assertj.core.api.Assertions.catchThrowable {
            steps.findForWorkflowAndId(WorkflowIdentifier(workflowId), StepIdentifier(id))
        }
        assertUnknownType(failure, TypeRole.VALUE, MODEL_TYPE_ALIAS)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `legacy mapped records report rejected values through the public migration failure contract`() {
        val id = ObjectId().toHexString()
        val workflowId = UUID.randomUUID().toString()
        steps.create(StepData(id, workflowId, "mapped-legacy", "1",
            mapOf("payload" to SecurityTestWorkflowValue("data")), Status.Active, emptyMap()))
        hostTemplate.getCollection(hostTemplate.getCollectionName(StepDocument::class.java))
            .updateOne(eq("_id", ObjectId(id)), combine(unset("data_entries"), unset("metadata_entries"),
                set("data.payload", Document("_class", WITNESS_NAME))))

        assertThatThrownBy { recordMigration.setup() }
            .isInstanceOf(MigrationError::class.java)
            .hasMessageContaining(id)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `mapped current records are not reprocessed by legacy migration`() {
        val id = ObjectId().toHexString()
        steps.create(StepData(id, UUID.randomUUID().toString(), "mapped-current", "1",
            mapOf("payload" to SecurityTestWorkflowValue("current")), Status.Active, emptyMap()))
        val collection = hostTemplate.getCollection(hostTemplate.getCollectionName(StepDocument::class.java))
        collection.updateOne(eq("_id", ObjectId(id)), set("data.payload", Document("_class", WITNESS_NAME)))
        val before = collection.find(eq("_id", ObjectId(id))).first()

        recordMigration.setup()

        assertThat(collection.find(eq("_id", ObjectId(id))).first()).isEqualTo(before)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `mapped legacy migration warns about rejected values and continues healthy jobs`() {
        val workflowId = UUID.randomUUID().toString()
        val healthy = listOf("before", "after").map { value ->
            JobData(ObjectId().toHexString(), workflowId, "mapped-job",
                mapOf("payload" to SecurityTestWorkflowValue(value)),
                Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        }
        val rejected = healthy.first().copy(id = ObjectId().toHexString())
        listOf(healthy.first(), rejected, healthy.last()).forEach { jobs.create(it) }
        val collection = hostTemplate.getCollection(hostTemplate.getCollectionName(JobDocument::class.java))
        (healthy + rejected).forEach {
            collection.updateOne(eq("_id", ObjectId(it.id)), unset("parameter_entries"))
        }
        collection.updateOne(eq("_id", ObjectId(rejected.id)),
            set("parameters.payload", Document("_class", WITNESS_NAME)))
        val rejectedBefore = collection.find(eq("_id", ObjectId(rejected.id))).first()

        warnMigration.setup()

        healthy.forEach {
            assertThat(jobs.findForWorkflowAndId(WorkflowIdentifier(workflowId), JobIdentifier(it.id))).isEqualTo(it)
            assertThat(collection.find(eq("_id", ObjectId(it.id))).first()).containsKey("parameter_entries")
        }
        assertThat(collection.find(eq("_id", ObjectId(rejected.id))).first()).isEqualTo(rejectedBefore)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `scheduled references honor mapped fields and converted status without materializing payloads`() {
        val workflowId = UUID.randomUUID().toString()
        val scheduled = listOf("before", "rejected", "after").map { value ->
            JobData(ObjectId().toHexString(), workflowId, "mapped-job",
                mapOf("payload" to SecurityTestWorkflowValue(value)),
                Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled)
        }
        scheduled.forEach { jobs.create(it) }
        val canceled = scheduled.first().copy(id = ObjectId().toHexString(), status = JobStatus.Canceled)
        jobs.create(canceled)
        val collection = hostTemplate.getCollection(hostTemplate.getCollectionName(JobDocument::class.java))
        collection.updateOne(eq("_id", ObjectId(scheduled[1].id)),
            set("parameters.payload", Document("_class", WITNESS_NAME)))
        assertThat(collection.find(eq("_id", ObjectId(scheduled.first().id))).first())
            .containsEntry("job_status", "status:Scheduled")
            .containsEntry("workflow_id", workflowId)

        val found = references.findScheduledJobReferences()

        assertThat(found.map { it.jobIdentifier.value }).containsExactlyInAnyOrderElementsOf(scheduled.map { it.id })
        assertThat(found.map { it.workflowIdentifier.value }).containsOnly(workflowId)
        assertThat(witnessLoader.events).isEmpty()
    }
}

@WritingConverter
object MappedJobStatusWriter : Converter<JobStatus, String> {
    override fun convert(source: JobStatus): String = "status:${source.name}"
}

@ReadingConverter
object MappedJobStatusReader : Converter<String, JobStatus> {
    override fun convert(source: String): JobStatus = JobStatus.valueOf(source.removePrefix("status:"))
}

@TestConfiguration
open class MappedMongoFieldsConfiguration {
    @Bean("mappedWarnMigration")
    open fun mappedWarnMigration(steps: StepRepository, jobs: JobRepository, access: FluxFlowMongoAccess): BootstrapAction =
        MigrateToTypeRecordsBootstrapAction(PartialFailureAction.Warn, steps, jobs, access.converter, access.template)

    companion object {
        @Bean
        @JvmStatic
        fun mappedMongoFields(): BeanPostProcessor = object : BeanPostProcessor {
            override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
                if (bean is MongoMappingContext) bean.setFieldNamingStrategy(SnakeCaseFieldNamingStrategy())
                return bean
            }
        }
    }
}
