package de.lise.fluxflow.persistence.migration

/**
 * A persistence that stores migrations.
 */
interface MigrationPersistence {
    /**
     * Persists a migration.
     * @param migration the migration to be persisted.
     * @return the persisted migration.
     * @throws DuplicateMigrationException if a different migration with the same key has already been persisted.
     */
    fun persist(migration: MigrationData): MigrationData

    /**
     * Finds a migration by its key.
     * @param key the key of the migration to be found.
     * @return the migration with the given key or `null` if no such migration exists.
     */
    fun find(key: String): MigrationData?
}