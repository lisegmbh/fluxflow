package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.Step

/**
 * An [InstanceAccessor] encapsulates the strategy for obtaining a concrete bound instance that backs a
 * [Step].
 *
 * It allows reflective / indirect code (e.g. metadata builders) to read and optionally transform
 * the instance associated with a step without needing to know how that instance is stored.
 *
 * Typical usage:
 *  val accessor = InstanceAccessor.fromStepInstance<MyStepType>()
 *  val stepInstance: MyStepType = accessor.get(step)
 *
 * You can derive further, typed accessors by chaining with [and]:
 *  val nameAccessor = accessor.and { it.name }
 *  val name: String = nameAccessor.get(step)
 *
 * Accessors are intentionally lightweight; composing them does not eagerly read the instance. Each
 * call to [get] will retrieve the step instance and apply all mapper functions in order.
 *
 * @param TInstance The type of the instance this accessor returns.
 */
fun interface InstanceAccessor<out TInstance> {
    /**
     * Returns the bound instance for the given [step]. Implementations decide how the instance is
     * retrieved (e.g. by calling `step.bind()` or accessing a cached value).
     *
     * @param step The step whose backing instance should be obtained.
     * @return The backing instance.
     */
    fun get(step: Step): TInstance

    /**
     * Creates a new [InstanceAccessor] that maps the instance produced by the current accessor
     * to another value using the provided [mapper].
     *
     * @param mapper A function that transforms the instance into another value.
     * @return A new accessor that returns the mapped value.
     */
    fun <TOther> and(
       mapper: (input: TInstance) -> TOther
    ): InstanceAccessor<TOther> {
        val current = this
        return InstanceAccessor {
            mapper(
                current.get(it)
            )
        }
    }

    companion object {
        /**
         * Creates an [InstanceAccessor] that retrieves the bound step instance via `Step.bind()`.
         *
         * This is the most common accessor used when the step instance itself is the root for
         * further introspection.
         *
         * @return An accessor returning the bound instance of a step.
         */
        fun <TInstance> fromStepInstance(): InstanceAccessor<TInstance> {
            return InstanceAccessor{
                it.bind()!!
            }
        }
    }
}