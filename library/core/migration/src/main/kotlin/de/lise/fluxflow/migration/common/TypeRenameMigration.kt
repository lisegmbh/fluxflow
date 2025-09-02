package de.lise.fluxflow.migration.common

import de.lise.fluxflow.migration.Migration
import de.lise.fluxflow.migration.MigrationKey
import kotlin.reflect.KClass

/**
 * A [TypeRenameMigration] can be used to migrate/rename an old type or kind.
 * @param originalName the original name as used before applying the migration.
 * @param newName the new name the original should be renamed to.
 */
open class TypeRenameMigration(
    val originalName: String,
    val newName: String,
    override val key: MigrationKey = MigrationKey("rename-${originalName}-to-${newName}")
) : Migration {
    /**
     * Creates a new [TypeRenameMigration].
     * @param originalName the original name as used before applying the migration.
     * @param newType the new type the original should be renamed to.
     */
    constructor(
        originalName: String,
        newType: KClass<*>
    ) : this(
        originalName,
        newType.qualifiedName ?: newType.java.canonicalName
    )
}