package de.lise.fluxflow.migration

/**
 * An [ExecutableMigration] is the executable representation of a [Migration].
 * It is used to execute the migration and created by the [MigrationProvider] that provides the migration.
 */
interface ExecutableMigration {
    /**
     * The declarative migration that will be applied when invoking [execute].
     */
    val migration: Migration

    /**
     * Execute the actual migration.
     */
    fun execute()
}