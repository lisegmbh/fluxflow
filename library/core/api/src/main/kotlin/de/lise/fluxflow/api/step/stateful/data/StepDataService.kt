package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.step.Step

/**
 * Service interface for accessing and modifying step data associated with a [Step] during workflow execution.
 *
 * A step data entry represents a piece of mutable or immutable state tied to a step instance.
 * Data entries may originate from public properties, annotated fields, or other metadata definitions,
 * depending on the step implementation.
 *
 * This interface provides a technology-neutral way to discover and manipulate such data entries at runtime.
 */
interface StepDataService {
    /**
     * Retrieves all data entries defined for the given [step].
     *
     * Each returned [Data] instance represents a named field (also known as the data's kind)
     * that holds part of the step's current state.
     *
     * @param step The step for which to retrieve data entries.
     * @return A list of [Data] objects representing the step’s data.
     */
    fun getData(step: Step): List<Data<*>>

    /**
     * Retrieves the data entry of the given [kind] from the specified [step].
     *
     * Throws an exception if the data kind does not exist.
     *
     * @param step The step containing the data.
     * @param kind The identifier (kind) of the data entry.
     * @return The corresponding [Data] object.
     * @throws DataNotFoundException if there is no step data for the provided [step] and [kind].
     */
    fun <T> getData(step: Step, kind: DataKind): Data<T>

    /**
     * Retrieves the data entry of the given [kind] from the specified [step], or returns `null` if not found.
     *
     * @param step The step containing the data.
     * @param kind The identifier (kind) of the data entry.
     * @return The corresponding [Data] object, or `null` if unavailable.
     */
    fun <T> getDataOrNull(step: Step, kind: DataKind): Data<T>?

    /**
     * Sets the value of the given [data] entry.
     *
     * The update is performed without additional context information.
     *
     * @param data The data entry to update.
     * @param value The new value to assign.
     */
    fun <T> setValue(data: Data<T>, value: T)

    /**
     * Assigns the given [value] to the [data] while also providing extra [context] information.
     *
     * @param context Arbitrary information to be passed along.
     * The provided value will (for example) be available on the emitted event (via [FlowEvent.context]).
     * @param value The value to be assigned.
     * @param data The data to be updated.
     */
    fun <T> setValue(context: Any, data: Data<T>, value: T)
}