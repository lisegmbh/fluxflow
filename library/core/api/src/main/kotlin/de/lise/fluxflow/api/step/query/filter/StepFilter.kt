package de.lise.fluxflow.api.step.query.filter

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.versioning.query.filter.VersionFilter
import de.lise.fluxflow.query.filter.Filter

data class StepFilter(
    val id: StepIdentifierFilter?,
    val definition: StepDefinitionFilter?,
    val metadata: Filter<Map<String, Any>>?,
    val status: Filter<Status>?,
    val version: VersionFilter? = null
)