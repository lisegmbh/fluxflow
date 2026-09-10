package de.lise.fluxflow.mongo.security.consumer

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.reflection.types.TypeRegistration
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import de.lise.fluxflow.springboot.types.TypeRegistrationContributor
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.job.JobPayload
import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.automation.OnCreated
import de.lise.fluxflow.stereotyped.versioning.Version
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Instant

data class ExternalWorkflowModel(
    var state: String = "new",
    var executedJobs: Int = 0,
)

@Step(WFMS_STEP_KIND)
@Version(WFMS_CURRENT_VERSION)
class WfmsReviewStep(
    private val model: ExternalWorkflowModel,
) {
    @OnCreated
    fun markCreated() {
        model.state = "review"
    }

    @Action
    fun scheduleReminder(): Continuation<*> = Continuation.job(
        Instant.EPOCH,
        WfmsReminderJob(model),
    )
}

@Job(WFMS_JOB_KIND)
class WfmsReminderJob(
    private val model: ExternalWorkflowModel,
) {
    @JobPayload
    fun execute() {
        model.executedJobs++
        model.state = "reminded"
    }
}

@Configuration
@Import(
    BasicConfiguration::class,
    MongoConfiguration::class,
)
@ComponentScan(basePackageClasses = [WfmsLikeConsumerConfiguration::class])
open class WfmsLikeConsumerConfiguration {
    @Bean
    @Primary
    open fun immediateSchedulingService(): SchedulingService = ImmediateSchedulingService()

    @Bean
    open fun externalWorkflowModelRegistration(): TypeRegistrationContributor =
        TypeRegistrationContributor {
            listOf(
                TypeRegistration(
                    TypeRole.MODEL,
                    ExternalWorkflowModel::class.java.name,
                    ExternalWorkflowModel::class,
                )
            )
        }
}

private class ImmediateSchedulingService : SchedulingService {
    private val callbacks = mutableListOf<SchedulingCallback>()

    override fun schedule(pointInTime: Instant, schedulingReference: SchedulingReference) {
        callbacks.toList().forEach { it.onScheduled(schedulingReference) }
    }

    override fun cancel(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey) = Unit

    override fun registerListener(callback: SchedulingCallback) {
        callbacks += callback
    }

    override fun isJobScheduled(schedulingReference: SchedulingReference): Boolean = false
}

const val WFMS_STEP_KIND = "wfms-review"
const val WFMS_JOB_KIND = "wfms-reminder"
const val WFMS_CURRENT_VERSION = "2"
const val WFMS_RESTORED_VERSION = "1"
