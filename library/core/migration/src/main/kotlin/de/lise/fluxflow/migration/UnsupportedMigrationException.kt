package de.lise.fluxflow.migration

class UnsupportedMigrationException(migration: Migration) : Exception(
    "There is not provider that supports the migration of the type '${migration::class.qualifiedName}' with the key '${migration.key.value}'."
)