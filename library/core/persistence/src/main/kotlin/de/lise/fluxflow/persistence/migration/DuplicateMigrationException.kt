package de.lise.fluxflow.persistence.migration

class DuplicateMigrationException(val key: String, cause: Throwable? = null)
    : RuntimeException("Migration with key $key already exists", cause)