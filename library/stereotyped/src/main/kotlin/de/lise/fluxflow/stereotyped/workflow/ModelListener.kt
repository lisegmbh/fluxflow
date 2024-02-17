package de.lise.fluxflow.stereotyped.workflow

/**
 * The `@ModelListener` is used to register a workflow model's function as a model listener.
 * Functions annotated with this annotation are invoked whenever the workflow's model changes.
 * @param value A selector that selects the relevant part of the workflow model,
 * that must change in order for this listener to be invoked.
 *
 * If the value is an empty string, any model change will trigger this listener.
 * The expression syntax depends on the current runtime.
 *
 * In Spring environments, this will usually be the Spring Expression Language.
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModelListener(
    val value: String = ""
)