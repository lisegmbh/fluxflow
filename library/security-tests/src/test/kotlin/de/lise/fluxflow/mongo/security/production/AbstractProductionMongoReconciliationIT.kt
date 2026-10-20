package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.engine.job.JobActivationException
import de.lise.fluxflow.stereotyped.job.JobConfigurationException
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobMongoPersistence
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import java.util.UUID

abstract class AbstractProductionMongoReconciliationIT {
    @Autowired private lateinit var runtimeJobs: JobService
    @Autowired private lateinit var runtimeWorkflows: WorkflowService

    @Autowired
    private lateinit var access: FluxFlowMongoAccess

    @Autowired
    private lateinit var workflows: WorkflowPersistence

    @Autowired
    private lateinit var jobs: JobPersistence

    @Autowired
    private lateinit var scheduledJobReferences: ScheduledJobReferencePersistence

    @Autowired
    @Qualifier("startupJobReconciliation")
    private lateinit var reconciliation: BootstrapAction

    @Autowired
    private lateinit var schedulingService: RecordingSchedulingService

    @BeforeEach
    fun clearPersistence() {
        schedulingService.scheduled.clear()
        ReconciliationInvalidJob.executions.set(0)
        access.template.remove(Query(), JobDocument::class.java)
        access.template.remove(Query(), WorkflowDocument::class.java)
    }

    @Test
    fun `O01 production startup isolates a job builder failure between healthy scheduled jobs`() {
        val workflowId = WorkflowIdentifier(UUID.randomUUID().toString())
        workflows.create(ReconciliationWorkflowModel("builder isolation"), workflowId)
        val identifiers = List(3) { ObjectId().toHexString() }
        val scheduledTime = Instant.parse("2099-09-10T10:00:00Z")
        identifiers.forEachIndexed { index, id ->
            jobs.create(JobData(id, workflowId.value,
                if (index == 1) RECONCILIATION_INVALID_JOB_KIND else RECONCILIATION_JOB_KIND,
                emptyMap(), scheduledTime, null, JobStatus.Scheduled))
        }
        val runtimeWorkflow = runtimeWorkflows.get<Any>(workflowId)

        val failure = catchThrowable { runtimeJobs.getJob(runtimeWorkflow, JobIdentifier(identifiers[1])) }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to activate job #${identifiers[1]} with kind '$RECONCILIATION_INVALID_JOB_KIND'")
            .hasCauseExactlyInstanceOf(JobConfigurationException::class.java)
        val before = jobCollection().find().map { it.toJson() }.sorted()
        assertThat(scheduledJobReferences.findScheduledJobReferences().map { it.jobIdentifier.value })
            .describedAs("The public reconciliation source must place the builder failure between the healthy jobs")
            .containsExactlyElementsOf(identifiers)

        reconciliation.setup()

        assertThat(schedulingService.scheduled.map { it.jobIdentifier.value })
            .containsExactlyInAnyOrder(identifiers.first(), identifiers.last())
        assertThat(ReconciliationInvalidJob.executions.get()).isZero()
        assertThat(jobCollection().find().map { it.toJson() }.sorted()).isEqualTo(before)
    }

    @Test
    fun `O01 production startup isolates an unknown payload between healthy scheduled jobs`() {
        assertThat(scheduledJobReferences)
            .isInstanceOf(JobMongoPersistence::class.java)
            .isSameAs(jobs)

        val workflowId = UUID.randomUUID().toString()
        val goodBeforeId = ObjectId().toHexString()
        val rejectedId = ObjectId().toHexString()
        val goodAfterId = ObjectId().toHexString()
        val scheduledTime = Instant.parse("2099-09-10T10:00:00Z")
        workflows.create(
            ReconciliationWorkflowModel("O01"),
            WorkflowIdentifier(workflowId),
        )

        listOf(goodBeforeId, rejectedId, goodAfterId).forEach { id ->
            jobs.create(
                JobData(
                    id,
                    workflowId,
                    RECONCILIATION_JOB_KIND,
                    emptyMap(),
                    scheduledTime,
                    null,
                    JobStatus.Scheduled,
                )
            )
        }
        jobCollection().updateOne(
            eq("_id", ObjectId(rejectedId)),
            set(
                "parameterEntries.values.payload",
                Document("_class", "unregistered.Payload"),
            ),
        )

        val regularReadFailure = runCatching {
            jobs.findForWorkflowAndId(
                WorkflowIdentifier(workflowId),
                JobIdentifier(rejectedId),
            )
        }.exceptionOrNull() ?: throw AssertionError("The normal job read must fail closed")
        val unknownType = generateSequence(regularReadFailure as Throwable?) { it.cause }
            .filterIsInstance<UnknownTypeException>()
            .singleOrNull()
        assertThat(unknownType).isNotNull
        assertThat(unknownType!!.role).isEqualTo(TypeRole.VALUE)
        assertThat(unknownType.key).isEqualTo("unregistered.Payload")

        reconciliation.setup()

        assertThat(schedulingService.scheduled.map { it.jobIdentifier.value })
            .containsExactlyInAnyOrder(goodBeforeId, goodAfterId)
    }

    private fun jobCollection() = access.template.getCollection(
        access.template.getCollectionName(JobDocument::class.java),
    )

    @Test
    fun `O01 rejected workflow does not prevent scheduling a later healthy workflow`() {
        val rejectedWorkflow = UUID.randomUUID().toString()
        val healthyWorkflow = UUID.randomUUID().toString()
        val rejectedIds = listOf(ObjectId().toHexString(), ObjectId().toHexString())
        val healthyIds = listOf(ObjectId().toHexString(), ObjectId().toHexString())
        listOf(rejectedWorkflow to rejectedIds, healthyWorkflow to healthyIds).forEach { (workflowId, ids) ->
            workflows.create(ReconciliationWorkflowModel("group"), WorkflowIdentifier(workflowId))
            ids.forEach { id -> jobs.create(JobData(id, workflowId, RECONCILIATION_JOB_KIND, emptyMap(),
                Instant.parse("2099-09-10T10:00:00Z"), null, JobStatus.Scheduled)) }
        }
        access.template.getCollection(access.template.getCollectionName(WorkflowDocument::class.java))
            .updateOne(eq("_id", rejectedWorkflow), set("model._class", "unregistered.WorkflowModel"))

        reconciliation.setup()

        assertThat(schedulingService.scheduled.map { it.jobIdentifier.value })
            .containsExactlyInAnyOrderElementsOf(healthyIds)
    }
}
