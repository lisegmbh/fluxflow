package de.lise.fluxflow.springboot.scheduling.quartz.ext

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.springboot.scheduling.quartz.JobDataMapKeys
import org.quartz.JobDetail

val JobDetail.jobIdentifier: JobIdentifier?
    get() {
        return this.jobDataMap[JobDataMapKeys.JOB_IDENTIFIER]?.let {
            it as? String
        }?.let {
            JobIdentifier(it)
        }
    }

val JobDetail.workflowIdentifier: WorkflowIdentifier?
    get() {
        return this.jobDataMap[JobDataMapKeys.WORKFLOW_IDENTIFIER]?.let { 
            it as? String
        }?.let {
            WorkflowIdentifier(it)
        }
    }