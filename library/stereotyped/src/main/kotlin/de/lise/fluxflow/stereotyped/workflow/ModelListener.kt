package de.lise.fluxflow.stereotyped.workflow

/**
 * The `@ModelListener` is used to register a workflow model's function as a model listener.
 * Functions annotated with this annotation are invoked whenever the workflow's model changes.
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModelListener