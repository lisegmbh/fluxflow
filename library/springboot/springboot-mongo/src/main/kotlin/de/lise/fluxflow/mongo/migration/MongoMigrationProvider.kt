package de.lise.fluxflow.mongo.migration

import de.lise.fluxflow.migration.ExecutableMigration
import de.lise.fluxflow.migration.Migration
import de.lise.fluxflow.migration.MigrationProvider
import de.lise.fluxflow.migration.common.TypeRenameMigration
import de.lise.fluxflow.mongo.migration.common.MongoExecutableTypeRenameMigration
import org.springframework.data.mongodb.core.MongoTemplate

class MongoMigrationProvider internal constructor(
    private val mongoTemplate: MongoTemplate,
    private val typeKey: String,
) : MigrationProvider {
    constructor(mongoTemplate: MongoTemplate) : this(mongoTemplate, "_class")

    override fun provide(migration: Migration): ExecutableMigration? {
        return when(migration) {
            is TypeRenameMigration -> MongoExecutableTypeRenameMigration(
                migration,
                false,
                mongoTemplate,
                typeKey,
            )
            else -> null
        }
    }
}
