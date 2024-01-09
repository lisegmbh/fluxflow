package de.lise.fluxflow.stereotyped.step.automation

/**
 * Functions annotated with this annotation are executed automatically during a step's lifecycle.
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Automated(
    val trigger: Trigger
)


