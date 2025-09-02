package de.lise.fluxflow.mongo.bootstrapping

/**
 * This enum is used to specify the behavior for a migration that partially failed to migrate some records.
 */
enum class PartialFailureAction {
    /**
     * The migration should fail and prevent the execution from continuing.
     */
    Fail,

    /**
     * The migration should warn about failed records and continue normally.
     * The consequences might differ depending on the actual migration.
     */
    Warn
}