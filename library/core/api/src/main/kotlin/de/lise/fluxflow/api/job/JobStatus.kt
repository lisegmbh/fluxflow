package de.lise.fluxflow.api.job

enum class JobStatus {
    Scheduled,
    Running,
    Executed,
    Canceled,
    Failed
}