package de.lise.fluxflow.migration

import org.slf4j.LoggerFactory

/**
 * A NoOpExecutableMigration is a migration that does not perform any actions.
 * It can be used by providers whenever they support a migration which doesn't require any actions to be taken.
 */
class NoOpExecutableMigration(
    override val migration: Migration
) : ExecutableMigration {
    override fun execute() {
        Logger.debug("No actions are taken for the migration with key '{}'.", migration.key.value)
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(NoOpExecutableMigration::class.java)!!
    }
}