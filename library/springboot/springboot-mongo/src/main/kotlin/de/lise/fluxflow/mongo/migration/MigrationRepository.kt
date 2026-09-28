package de.lise.fluxflow.mongo.migration

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface MigrationRepository : MongoRepository<MigrationDocument, String> {
    fun findByKey(key: String): MigrationDocument?
}
