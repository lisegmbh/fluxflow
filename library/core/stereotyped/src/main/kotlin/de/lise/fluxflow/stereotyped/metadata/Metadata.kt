package de.lise.fluxflow.stereotyped.metadata

import de.lise.fluxflow.api.step.StepDefinition

/**
 * This annotation can be used to customize the serialization of metadata annotations.
 *
 * In order to apply this to annotation parameters, use:
 * 
 * ```kotlin
 * `@get:Metadata(...)`
 * ```
 * The behavior is defined as follows:
 * - If the annotation is present on a parameterless annotation, the given [key] will be used.
 * - If the annotation is present on a parameter and
 *    - the annotation itself is not annotated, the property's annotation [key] will be used.
 *    - the annotation itself is also annotated, a concatenation of the annotation's and property's [key] will be used (annotation key + "." + property key).
 * 
 * @param key The key that should be used when determining the key required for the [StepDefinition.metadata] map.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Metadata(
    val key: String
)
