package de.lise.fluxflow.springboot.rest.workflow.step.data

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.rest.workflow.step.data.StepDataMetadataDto

@ExperimentalApi
data class DefaultStepDataMetadataDto(
    override val workflowIdentifier: String,
    override val stepIdentifier: String,
    override val kind: String,
    override val readonly: Boolean,
    override val annotations: Map<String, Any>
) : StepDataMetadataDto