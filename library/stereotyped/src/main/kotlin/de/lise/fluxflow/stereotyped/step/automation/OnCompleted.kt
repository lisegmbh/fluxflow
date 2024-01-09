package de.lise.fluxflow.stereotyped.step.automation
import de.lise.fluxflow.api.step.Status
/**
 * Functions that are annotated with this annotation are executed automatically
 * when a workflow step transitions into the [Status.Completed] status.
 *
 * This is essentially the same as annotating it with `@Automated(Trigger.OnCompleted)`.
 *
 * @see Automated
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnCompleted
