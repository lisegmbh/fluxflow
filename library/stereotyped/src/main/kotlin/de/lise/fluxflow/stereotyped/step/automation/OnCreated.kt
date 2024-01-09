package de.lise.fluxflow.stereotyped.step.automation

/**
 * Functions that are annotated with this annotation are executed automatically
 * when a workflow step is initially created.
 *
 * This is essentially the same as annotating it with `@Automated(Trigger.OnCreated)`.
 *
 * @see Automated
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnCreated