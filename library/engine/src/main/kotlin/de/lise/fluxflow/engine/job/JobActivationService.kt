package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobDefinition
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder

class JobActivationService(
    private val iocProvider: IocProvider,
    private val jobDefinitionBuilder: JobDefinitionBuilder,
    private val classLoaderProvider: ClassLoaderProvider,
) {
    fun <TWorkflowModel> activate(
        workflow: Workflow<TWorkflowModel>,
        jobData: JobData
    ): Job {
        return activateJobDefinition(workflow, jobData).createJob(
            JobIdentifier(jobData.id),
            workflow,
            jobData.scheduledTime,
            jobData.cancellationKey?.let { CancellationKey(it) },
            jobData.status
        )
    }

    fun toJobDefinition(definitionObject: Any): JobDefinition {
        return definitionObject
            .takeIf { it is JobDefinition }
            ?.let { it as JobDefinition }
            ?: jobDefinitionBuilder.build(definitionObject)
    }

    private fun <TWorkflowModel> activateJobDefinition(
        workflow: Workflow<TWorkflowModel>,
        jobData: JobData
    ): JobDefinition {
        return JobActivation(
            classLoaderProvider.provide(),
            jobDefinitionBuilder,
            iocProvider,
            workflow,
            jobData
        ).activate()
    }
}