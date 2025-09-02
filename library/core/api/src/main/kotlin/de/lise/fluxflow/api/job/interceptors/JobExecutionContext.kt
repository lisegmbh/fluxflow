package de.lise.fluxflow.api.job.interceptors

import de.lise.fluxflow.api.job.Job

data class JobExecutionContext(
    val job: Job
)