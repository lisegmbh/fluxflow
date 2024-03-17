package de.lise.fluxflow.migration

/**
 * A migration describes a set of changes to be applied to FluxFlow's current state.
 */
interface Migration {
    /**
     * The key that identifies this migration.
     */
    val key: MigrationKey
}