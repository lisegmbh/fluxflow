package de.lise.fluxflow.stereotyped.versioning

/**
 * The @[Version] annotation can be used to manually specify the version of a workflow element's definition.
 * This is currently only supported by steps.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class Version(
    /**
     * A string that identifies the current version.
     */
    val value: String,
    /**
     * A list of older versions, which are known to be compatible to this version.
     */
    val compatibleVersions: Array<String> = []
)