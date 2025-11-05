package de.lise.fluxflow.stereotyped

import de.lise.fluxflow.stereotyped.PrefixStrategy.CamelCase
import de.lise.fluxflow.stereotyped.PrefixStrategy.Plain

/**
 * Defines strategies for applying a prefix to imported names when using the [Import] annotation.
 *
 * This enum controls how the prefix is combined with the imported name:
 *
 * - [Plain]: Uses the prefix exactly as specified, without modification.
 * - [CamelCase]: Adapts the prefix in camel case style to fit the imported name (capitalizes the first letter of the name).
 *
 * These strategies help avoid naming conflicts and improve code readability when importing elements from other classes or sources.
 */
enum class PrefixStrategy {
    /**
     * Uses the prefix exactly as specified, without modification.
     *
     * **Example:**
     *
     * | Prefix | Name | Result   |
     * |--------|------|----------|
     * | foo    | bar  | foobar   |
     */
    Plain,

    /**
     * Adapts the prefix in camel case style to fit the imported name (capitalizes the first letter of the name).
     *
     * **Example:**
     *
     * | Prefix | Name | Result   |
     * |--------|------|----------|
     * | foo    | bar  | fooBar   |
     */
    CamelCase;

    /**
     * Applies the prefix to the given name according to the selected strategy.
     *
     * @param prefix The prefix to apply.
     * @param nameToBePrefixed The name to which the prefix should be applied.
     * @return The resulting name with the prefix applied.
     */
    fun apply(
        prefix: String,
        nameToBePrefixed: String,
    ): String {
        if (prefix.isBlank()) {
            return nameToBePrefixed
        }
        return when (this) {
            Plain -> "$prefix$nameToBePrefixed"
            CamelCase -> "$prefix${capitalize(nameToBePrefixed)}"
        }
    }

    /**
     * Removes the given prefix from the start of the provided name, if present.
     *
     * @param prefix The prefix to remove.
     * @param prefixedName The name from which the prefix should be removed.
     * @return The name without the prefix, or the original name if the prefix was not present.
     */
    fun remove(
        prefix: String,
        prefixedName: String,
    ): String {
        return when (this) {
            Plain -> prefixedName.removePrefix(prefix)
            CamelCase -> uncapitalize(
                prefixedName.removePrefix(prefix)
            )
        }
    }

    /**
     * Checks if the given value starts with the specified prefix according to the selected strategy.
     *
     * @param prefix The prefix to check for.
     * @param value The value to check.
     * @return True if the value starts with the prefix, false otherwise.
     */
    fun isPrefixed(
        prefix: String,
        value: String,
    ): Boolean {
        return when (this) {
            else -> value.startsWith(prefix)
        }
    }

    private fun capitalize(value: String): String {
        return when (value.length) {
            0 -> value
            1 -> value.uppercase()
            else -> "${value.take(1).uppercase()}${value.substring(1)}"
        }
    }

    private fun uncapitalize(value: String): String {
        return when (value.length) {
            0 -> value
            1 -> value.lowercase()
            else -> "${value.take(1).lowercase()}${value.substring(1)}"
        }
    }
}