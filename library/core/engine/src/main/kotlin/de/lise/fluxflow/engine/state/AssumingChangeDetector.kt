package de.lise.fluxflow.engine.state

import de.lise.fluxflow.api.state.ChangeDetector

/**
 * The [AssumingChangeDetector] always assumes that an entity did change if [assumedResult] is set to `true`.
 * If it is set to `false`, it always assumes that it did not change.
 */
class AssumingChangeDetector<in TEntity>(
    private val assumedResult: Boolean
): ChangeDetector<TEntity> {
    override fun hasChanged(oldVersion: TEntity, newVersion: TEntity): Boolean {
        return assumedResult
    }
}