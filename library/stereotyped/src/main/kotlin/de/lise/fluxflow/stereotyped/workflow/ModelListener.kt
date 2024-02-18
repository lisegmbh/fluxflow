package de.lise.fluxflow.stereotyped.workflow

/**
 * The `@ModelListener` is used to register a workflow model's function as a model listener.
 * Functions annotated with this annotation are invoked whenever the workflow's model changes.
 * @param selector A selector expression that selects the relevant aspect of a workflow's model,
 * which must change in order for this listener to be invoked.
 *
 * If the value is an empty string, any model change will trigger this listener.
 * The expression syntax depends on the current runtime.
 *
 * In Spring environments, the expression syntax will usually be the Spring Expression Language.
 * @param selectorReturnsDecision If set to `true`, the selector should return a boolean value.
 *
 * The result will then directly indicate if there are relevant changes to the model.
 *
 * If the parameter is set to false (which is the default),
 * the expression is expected to return any part of the model and will be invoked twice.
 * Once for the "old" model and a second time for the updated model.
 * Only if the returned values differ, the listener will be invoked.
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModelListener(
    val selector: String = "",
    val selectorReturnsDecision: Boolean = false,
)