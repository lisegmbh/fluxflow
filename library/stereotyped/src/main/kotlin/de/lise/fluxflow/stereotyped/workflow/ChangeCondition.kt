package de.lise.fluxflow.stereotyped.workflow

/**
 * A [ChangeCondition] can be used to determine if a listener should be invoked.
 * @param TModel The model type this condition is able to process.
 */
fun interface ChangeCondition<in TModel> {
    /**
     * Returns `true`, if the provided model has changed in a way, that is relevant to a model listener.
     * @param oldModel The old model version.
     * @param newModel The new model version.
     * @return `true`, if there has been a relevant change from [oldModel] to [newModel].
     */
    fun isSatisfied(oldModel: TModel, newModel: TModel): Boolean

    companion object{
        /**
         * A condition that always returns `false`.
         */
        val False = ChangeCondition<Any?> { _, _ -> false }

        /**
         * A condition that always returns `true`.
         */
        val True = ChangeCondition<Any?> { _, _ -> true }

        /**
         * Returns a new [ChangeCondition] that returns `true`, if any of the provided [conditions] return true.
         * @param conditions The conditions that should be wrapped.
         * @return `true`, if at least one condition within [conditions] returns `true`.
         */
        fun <TModel> any(conditions: List<ChangeCondition<TModel>>): ChangeCondition<TModel> {
            return when(conditions.size) {
                0 -> False
                1 -> conditions[0]
                else -> ChangeCondition { old, new ->
                    conditions.any {
                        it.isSatisfied(old, new)
                    }
                }
            }
        }
    }
}