package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.rest.workflow.step.StepMetadataDto

data class DefaultStepMetadataDto(
    override val workflowIdentifier: String,
    override val stepIdentifier: String,
    override val kind: String,
    override val annotations: Map<String, Any>
) : StepMetadataDto