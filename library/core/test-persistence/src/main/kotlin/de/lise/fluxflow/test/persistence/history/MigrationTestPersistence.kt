package de.lise.fluxflow.test.persistence.history

import de.lise.fluxflow.persistence.migration.DuplicateMigrationException
import de.lise.fluxflow.persistence.migration.MigrationData
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import de.lise.fluxflow.test.persistence.TestIdGenerator

class MigrationTestPersistence(
    private val idGenerator: TestIdGenerator = TestIdGenerator(),
    private val entities: MutableMap<String, MigrationData> = mutableMapOf()
) : MigrationPersistence {
    override fun persist(migration: MigrationData): MigrationData {
        var migrationToPersist = migration
        if (migrationToPersist.id == null) {
            if(entities.values.any { it.key == migration.key }){
                throw DuplicateMigrationException(migration.key)
            }
            migrationToPersist = migrationToPersist.copy(
                id = idGenerator.newId()
            )
        }
        entities[migrationToPersist.id!!] = migrationToPersist
        return migrationToPersist
    }
}