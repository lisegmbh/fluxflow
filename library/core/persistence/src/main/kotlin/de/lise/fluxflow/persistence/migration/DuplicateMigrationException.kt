package de.lise.fluxflow.persistence.migration

class DuplicateMigrationException(val key: String)
    : RuntimeException("Migration with key $key already exists")