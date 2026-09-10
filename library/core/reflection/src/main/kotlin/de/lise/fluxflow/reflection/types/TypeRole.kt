package de.lise.fluxflow.reflection.types

/**
 * The purpose for which a persisted type identifier may be resolved.
 */
enum class TypeRole(
    internal val manifestName: String,
) {
    STEP("step"),
    JOB("job"),
    MODEL("model"),
    VALUE("value");

    internal companion object {
        fun fromManifestName(value: String): TypeRole? = entries.singleOrNull {
            it.manifestName == value
        }
    }
}
