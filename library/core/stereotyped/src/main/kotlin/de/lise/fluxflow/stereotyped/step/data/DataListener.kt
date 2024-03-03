package de.lise.fluxflow.stereotyped.step.data

/**
 * The `@DataListener` is used to register a step function as a step data listener.
 * Functions annotated with this annotation are invoked,
 * whenever the step's data with the given [dataKind], changes.
 *
 * You may repeat this annotation for different step kinds in order to reuse a function for various step data.
 *
 * @param dataKind The kind of step data to be listened to.
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataListener(
    val dataKind: String
)