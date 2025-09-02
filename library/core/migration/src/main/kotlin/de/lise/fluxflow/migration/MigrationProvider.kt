package de.lise.fluxflow.migration

/**
 * A [MigrationProvider] is responsible for providing an [ExecutableMigration] for a given [Migration].
 */
interface MigrationProvider {
    /**
     * Returns the executable representation of the given [migration].
     * If the provider is not able to provide an executable migration for the given migration, it should return `null`.
     * @param migration the migration to provide an executable representation for.
     * @return the executable representation of the given [migration] or `null` if the provider is not able to provide an executable migration for the given migration.
     */
    fun provide(migration: Migration): ExecutableMigration?
}