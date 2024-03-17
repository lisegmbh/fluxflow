package de.lise.fluxflow.migration

import de.lise.fluxflow.persistence.migration.MigrationData
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import org.slf4j.LoggerFactory
import java.time.Clock

class MigrationServiceImpl(
    private val persistence: MigrationPersistence,
    private val providers: List<MigrationProvider>,
    private val clock: Clock
) : MigrationService {
    override fun apply(migration: Migration) {
        persistence.find(migration.key.value)?.let {
            Logger.info(
                "Migration with key '{}' (id: {}) has already been applied at {}.",
                migration.key.value,
                it.id,
                it.executionTime
            )
            return
        }

        val executableMigration = providers
            .firstNotNullOfOrNull { it.provide(migration) }
            ?: throw UnsupportedMigrationException(migration)

        persistence.persist(
            MigrationData(
                null,
                migration.key.value,
                clock.instant()
            )
        )

        Logger.debug(
            "Executing migration of type '{}' and with key '{}'.",
            migration::class.qualifiedName,
            migration.key.value
        )
        executableMigration.execute()
        Logger.debug(
            "Migration of type '{}' and with key '{}' has been executed successfully.",
            migration::class.qualifiedName,
            migration.key.value
        )
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(MigrationService::class.java)!!
    }
}