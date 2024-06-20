package de.lise.fluxflow.test.persistence.migration

import de.lise.fluxflow.migration.ExecutableMigration
import de.lise.fluxflow.migration.Migration
import de.lise.fluxflow.migration.MigrationProvider
import de.lise.fluxflow.migration.NoOpExecutableMigration
import de.lise.fluxflow.migration.common.TypeRenameMigration

class InMemoryMigrationProvider : MigrationProvider {
    override fun provide(migration: Migration): ExecutableMigration? {
        // because the persistence provider is in-memory, it doesn't require migration based on its nature
        return when (migration) {
            is TypeRenameMigration -> NoOpExecutableMigration(migration)
            else -> null
        }
    }
}