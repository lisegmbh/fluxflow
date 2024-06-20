package de.lise.fluxflow.migration

/**
 * A [MigrationError] indicates that a vital part of a migration failed and the execution should be stopped to
 * avoid data loss or corruption.
 */
class MigrationError : Error {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}