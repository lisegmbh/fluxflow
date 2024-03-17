package de.lise.fluxflow.mongo.migration

import org.springframework.data.mongodb.repository.MongoRepository

interface MigrationRepository : MongoRepository<MigrationDocument, String> {
    fun findByKey(key: String): MigrationDocument?
}