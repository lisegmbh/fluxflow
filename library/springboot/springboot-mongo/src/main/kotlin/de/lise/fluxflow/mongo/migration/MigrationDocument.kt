package de.lise.fluxflow.mongo.migration

import de.lise.fluxflow.persistence.migration.MigrationData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class MigrationDocument(
    @Id var id: String?,
    val key: String,
    val executionTime: Instant
) {
    constructor(migration: MigrationData) : this(
        migration.id,
        migration.key,
        migration.executionTime
    )

    fun toMigrationData(): MigrationData {
        return MigrationData(
            id,
            key,
            executionTime
        )
    }
}