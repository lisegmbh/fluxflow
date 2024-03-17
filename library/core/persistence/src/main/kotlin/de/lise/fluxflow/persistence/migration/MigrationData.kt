package de.lise.fluxflow.persistence.migration

import java.time.Instant

data class MigrationData(
    val id: String?,
    val key: String,
    val executionTime: Instant
)