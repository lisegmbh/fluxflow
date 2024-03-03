package de.lise.fluxflow.engine.state

import de.lise.fluxflow.api.state.ChangeDetector

class DefaultChangeDetector<in TEntity> : ChangeDetector<TEntity> {
    override fun hasChanged(
        oldVersion: TEntity,
        newVersion: TEntity
    ): Boolean {
        return oldVersion != newVersion
    }
}

