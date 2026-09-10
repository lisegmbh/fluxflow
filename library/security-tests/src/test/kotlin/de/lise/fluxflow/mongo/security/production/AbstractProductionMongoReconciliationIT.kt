package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
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
        access.template.remove(Query(), JobDocument::class.java)
        access.template.remove(Query(), WorkflowDocument::class.java)
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
}
