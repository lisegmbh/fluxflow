package de.lise.fluxflow.migration

/**
 * A service that applies migrations.
 */
interface MigrationService {
    /**
     * Applies the given migration.
     * If the migration has already been applied (evaluated using the [Migration.key]), this method does nothing.
     * @param migration the migration to be applied.
     */
    fun apply(migration: Migration)
}