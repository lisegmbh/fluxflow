package de.lise.fluxflow.mongo.migration

import com.mongodb.DuplicateKeyException
import de.lise.fluxflow.persistence.migration.DuplicateMigrationException
import de.lise.fluxflow.persistence.migration.MigrationData
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import org.springframework.data.mongodb.core.MongoTemplate

class MigrationMongoPersistence(
    private val repository: MigrationRepository,
    private val mongoTemplate: MongoTemplate
) : MigrationPersistence {
    override fun persist(migration: MigrationData): MigrationData {
        try {
            return mongoTemplate.save(MigrationDocument(migration)).toMigrationData()
        } catch (e: DuplicateKeyException) {
            throw DuplicateMigrationException(migration.key, e)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            throw DuplicateMigrationException(migration.key, e)
        }
    }

    override fun find(key: String): MigrationData? {
        return repository.findByKey(key)?.toMigrationData()
    }
}