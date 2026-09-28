package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.reflection.types.TypeRegistration
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.springboot.types.TypeRegistrationContributor
import de.lise.fluxflow.stereotyped.job.Job
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

internal const val RECONCILIATION_JOB_KIND = "security-reconciliation-job"

internal data class ReconciliationWorkflowModel(val value: String)

@Job(RECONCILIATION_JOB_KIND)
internal class ReconciliationJob {
    fun execute() = Unit
}

@TestConfiguration
open class ProductionMongoReconciliationConfiguration {
    @Bean
    open fun reconciliationTypes(): TypeRegistrationContributor =
        TypeRegistrationContributor {
            listOf(
                TypeRegistration(
                    TypeRole.MODEL,
                    ReconciliationWorkflowModel::class.java.name,
                    ReconciliationWorkflowModel::class,
                ),
                TypeRegistration(
                    TypeRole.JOB,
                    RECONCILIATION_JOB_KIND,
                    ReconciliationJob::class,
                ),
            )
        }

    @Bean
    @Primary
    open fun recordingSchedulingService(): RecordingSchedulingService =
        RecordingSchedulingService()
}

class RecordingSchedulingService : SchedulingService {
    val scheduled = CopyOnWriteArrayList<SchedulingReference>()

    override fun schedule(pointInTime: Instant, schedulingReference: SchedulingReference) {
        scheduled += schedulingReference
    }

    override fun cancel(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey) = Unit

    override fun registerListener(callback: SchedulingCallback) = Unit

    override fun isJobScheduled(schedulingReference: SchedulingReference): Boolean = false
}
