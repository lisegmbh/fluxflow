package de.lise.fluxflow.stereotyped

/**
 * Annotation that enables importing and reusing data properties from another class into a step.
 *
 * When applied to a property in a step class, this annotation tells FluxFlow to import all
 * declared elements (properties annotated with `@Data`) from the property's type and make
 * them available as if they were directly declared on the step class.
 *
 * This feature allows developers to encapsulate common data structures and reuse them across
 * multiple workflows, reducing code duplication and improving maintainability.
 *
 * ## Example
 *
 * ```kotlin
 * data class EmployeeInformation(
 *     @Data
 *     val name: String,
 *     @Data
 *     val socialSecurityNumber: String
 * )
 *
 * @Step
 * class RequestVacationStep(
 *     @Import
 *     val employeeInformation: EmployeeInformation
 * )
 * ```
 *
 * The above example behaves exactly the same as:
 *
 * ```kotlin
 * @Step
 * class RequestVacationStep(
 *     @Data
 *     val name: String,
 *     @Data
 *     val socialSecurityNumber: String
 * )
 * ```
 *
 * @param prefix Optional prefix to add to imported property names to avoid naming conflicts.
 *               When specified, all imported properties will be prefixed with this string.
 * @param prefixStrategy Specifies how the prefix parameter is interpreted and applied to the name to be prefixed.
 * Defaults to [PrefixStrategy.CamelCase].
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Import(
    val prefix: String = "",
    val prefixStrategy: PrefixStrategy = PrefixStrategy.CamelCase
)
