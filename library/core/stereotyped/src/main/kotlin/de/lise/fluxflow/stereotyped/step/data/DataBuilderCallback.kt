package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataDefinition

typealias DataBuilderCallback<T> = (instance: T) -> DataDefinition<*>