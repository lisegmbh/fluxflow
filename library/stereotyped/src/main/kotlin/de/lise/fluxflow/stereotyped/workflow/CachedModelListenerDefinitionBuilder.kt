package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.ModelListenerDefinition

internal typealias CachedModelListenerDefinitionBuilder<TModel> = (TModel) -> ModelListenerDefinition<TModel>