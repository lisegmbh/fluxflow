package de.lise.fluxflow.stereotyped.workflow

/**
 * A [SelectorExpression] represents a parsed and invokable version of [ModelListener.selector].
 */
fun interface SelectorExpression {
    /**
     * Evaluates the expression based on the old, new and current model version.
     * @param oldModel The old model state.
     * @param newModel The new model state.
     * @param currentModel If the expression should be used as a selector
     * ([ModelListener.selectorReturnsDecision] is set to `false`),
     * this parameter will contain the model state on which the selector should be applied.
     * @return the result value from evaluating this selector.
     */
    fun evaluate(
        oldModel: Any?,
        newModel: Any?,
        currentModel: Any?
    ): Any?
}