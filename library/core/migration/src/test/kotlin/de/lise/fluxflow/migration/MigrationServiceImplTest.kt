package de.lise.fluxflow.migration

import de.lise.fluxflow.persistence.migration.MigrationData
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Instant

class MigrationServiceImplTest {
    @Test
    fun `apply should do nothing if the migration has already been applied`() {
        // Arrange
        val migrationKey = MigrationKey("migrationKey")
        val migration = mock<Migration> {
            on { key } doReturn migrationKey
        }
        val provider = mock<MigrationProvider> {}
        val persistence = mock<MigrationPersistence> {
            on { find(eq(migrationKey.value)) } doReturn MigrationData("test", migrationKey.value, Instant.now())
        }
        val service = MigrationServiceImpl(
            persistence,
            listOf(provider),
            Clock.systemDefaultZone()
        )

        // Act
        service.apply(migration)

        // Assert
        verify(persistence, never()).persist(any())
        verify(provider, never()).provide(any())
    }

    @Test
    fun `apply should throw a UnsupportedMigrationException if there is no provider that supports the given migration`() {
        // Arrange
        val migrationKey = MigrationKey("migrationKey")
        val migration = mock<Migration> {
            on { key } doReturn migrationKey
        }
        val provider = mock<MigrationProvider> {
            on { provide(any()) } doReturn null
        }
        val persistence = mock<MigrationPersistence> {
            on { find(any()) } doReturn null
        }
        val service = MigrationServiceImpl(
            persistence,
            listOf(provider),
            Clock.systemDefaultZone()
        )

        // Act & Assert
        assertThrows<UnsupportedMigrationException> {
            service.apply(migration)
        }
    }

    @Test
    fun `apply should persist a migration before it is executed`() {
        // Arrange
        val migrationKey = MigrationKey("migrationKey")
        val migration = mock<Migration> {
            on { key } doReturn migrationKey
        }
        val executableMigration = mock<ExecutableMigration> {}
        val provider = mock<MigrationProvider> {
            on { provide(any()) } doReturn executableMigration
        }
        val persistence = mock<MigrationPersistence> {
            on { find(any()) } doReturn null
        }
        val clock = mock<Clock> {
            on { instant() } doReturn Instant.now()
        }
        val service = MigrationServiceImpl(
            persistence,
            listOf(provider),
            clock
        )

        // Act
        service.apply(migration)

        // Assert
        verify(persistence, times(1)).persist(
            eq(
                MigrationData(
                    null,
                    migrationKey.value,
                    clock.instant()
                )
            )
        )
    }

    @Test
    fun `apply should use the first providers result that is not null`() {
        // Arrange
        val migrationKey = MigrationKey("migrationKey")
        val migration = mock<Migration> {
            on { key } doReturn migrationKey
        }

        val provider1 = mock<MigrationProvider> {
            on { provide(any()) } doReturn null
        }
        val migration2 = mock<ExecutableMigration> {}
        val provider2 = mock<MigrationProvider> {
            on { provide(any()) } doReturn migration2
        }

        val migration3 = mock<ExecutableMigration> {}
        val provider3 = mock<MigrationProvider> {
            on { provide(any()) } doReturn migration3
        }

        val persistence = mock<MigrationPersistence> {
            on { find(any()) } doReturn null
        }
        val service = MigrationServiceImpl(
            persistence,
            listOf(provider1, provider2, provider3),
            Clock.systemDefaultZone()
        )

        // Act
        service.apply(migration)

        // Assert
        verify(provider1, times(1)).provide(migration)
        verify(provider2, times(1)).provide(migration)
        verify(provider3, never()).provide(any())
        verify(migration2, times(1)).execute()
    }
}