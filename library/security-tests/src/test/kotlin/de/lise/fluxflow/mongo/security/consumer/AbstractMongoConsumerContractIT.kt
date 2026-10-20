package de.lise.fluxflow.mongo.security.consumer

import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.RestoredStep
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.migration.MigrationDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query

abstract class AbstractMongoConsumerContractIT {
    @Autowired
    private lateinit var template: MongoTemplate

    @Autowired
    private lateinit var registry: TypeRegistry

    @Autowired
    private lateinit var workflowStarter: WorkflowStarterService

    @Autowired
    private lateinit var workflowQueries: WorkflowQueryService

    @Autowired
    private lateinit var stepService: StepService

    @Autowired
    private lateinit var actionService: ActionService

    @Autowired
    private lateinit var stepPersistence: StepPersistence

    @Autowired
    private lateinit var jobPersistence: JobPersistence

    @Autowired
    private lateinit var stepDefinitions: StepDefinitionPersistence

    @BeforeEach
    fun clearCollections() {
        listOf(
            WorkflowDocument::class.java,
            StepDocument::class.java,
            JobDocument::class.java,
            StepDefinitionDocument::class.java,
            ContinuationRecordDocument::class.java,
            MigrationDocument::class.java,
        ).forEach { type ->
            if (template.collectionExists(type)) {
                template.remove(Query(), type)
            }
        }
    }

    @Test
    fun `O04 resumes an external model through a custom step query and custom job execution`() {
        assertThat(
            registry.resolve(TypeRole.MODEL, ExternalWorkflowModel::class.java.name)
        ).isEqualTo(ExternalWorkflowModel::class)
        assertThat(registry.resolve(TypeRole.STEP, WFMS_STEP_KIND)).isEqualTo(WfmsReviewStep::class)
        assertThat(registry.resolve(TypeRole.JOB, WFMS_JOB_KIND)).isEqualTo(WfmsReminderJob::class)

        val model = ExternalWorkflowModel()
        val started = workflowStarter.start(model, Continuation.step(WfmsReviewStep(model)))
        val workflowId = started.identifier
        val reloaded = workflowQueries.get<ExternalWorkflowModel>(workflowId)

        assertThat(reloaded.model.state).isEqualTo("review")
        val step = stepService.findSteps(reloaded).single() as StatefulStep
        actionService.invokeAction(step.actions.single())

        val afterJob = workflowQueries.get<ExternalWorkflowModel>(workflowId)
        assertThat(afterJob.model.state).isEqualTo("reminded")
        assertThat(afterJob.model.executedJobs).isEqualTo(1)
        val executedJob = jobPersistence.findForWorkflow(workflowId).single()
        assertThat(executedJob.kind).isEqualTo(WFMS_JOB_KIND)
        assertThat(executedJob.status).isEqualTo(JobStatus.Executed)
    }

    @Test
    fun `O04 restores a persisted incompatible custom step without actions`() {
        val model = ExternalWorkflowModel()
        val started = workflowStarter.start(model, Continuation.step(WfmsReviewStep(model)))
        val workflowId = started.identifier
        val persisted = stepPersistence.findForWorkflow(workflowId).single()
        stepDefinitions.save(
            StepDefinitionData(
                WFMS_STEP_KIND,
                WFMS_RESTORED_VERSION,
                emptyMap(),
                emptyList(),
            )
        )
        stepPersistence.save(persisted.copy(version = WFMS_RESTORED_VERSION))

        val reloaded = workflowQueries.get<ExternalWorkflowModel>(workflowId)
        val restored = stepService.findSteps(reloaded).single()

        assertThat(restored).isInstanceOf(RestoredStep::class.java)
        assertThat((restored as StatefulStep).actions).isEmpty()
        assertThat(restored.version.version).isEqualTo(WFMS_RESTORED_VERSION)
    }
}
