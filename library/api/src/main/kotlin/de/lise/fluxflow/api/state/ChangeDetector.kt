package de.lise.fluxflow.api.state

/**
 * The [ChangeDetector] is responsible for detecting, if two versions of the same entity have changed.
 * @param TEntity The entity that can be tracked by this detector.
 */
fun interface ChangeDetector<in TEntity> {
    /**
     * Checks if the provided representations of a given entity are different from each other.
     * @param oldVersion The entity's older representation/state.
     * @param newVersion The entity's newer representation/state.
     * @return `true`, if the new version differs from the old version.
     */
    fun hasChanged(oldVersion: TEntity, newVersion: TEntity): Boolean
}