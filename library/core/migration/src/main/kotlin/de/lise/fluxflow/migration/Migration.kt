package de.lise.fluxflow.migration

import de.lise.fluxflow.migration.common.TypeRenameMigration
import kotlin.reflect.KClass

/**
 * A migration describes a set of changes to be applied to FluxFlow's current state.
 */
interface Migration {
    /**
     * The key that identifies this migration.
     */
    val key: MigrationKey

    companion object {
        /**
         * Creates a new [TypeRenameMigration].
         * @param oldName the original name as used before applying the migration.
         * @param newName the new name the original should be renamed to.
         * @return a new [TypeRenameMigration] instance.
         */
        @JvmStatic
        fun renameType(oldName: String, newName: String): TypeRenameMigration {
            return TypeRenameMigration(oldName, newName)
        }

        /**
         * Creates a new [TypeRenameMigration].
         * @param oldName the original name as used before applying the migration.
         * @param newType the new type the original should be renamed to.
         * @return a new [TypeRenameMigration] instance.
         */
        @JvmStatic
        fun renameType(oldName: String, newType: KClass<*>): TypeRenameMigration {
            return TypeRenameMigration(oldName, newType)
        }
    }
}