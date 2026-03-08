package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.rest.workflow.step.StepDto
import de.lise.fluxflow.rest.workflow.step.StepMetadataDto
import de.lise.fluxflow.rest.workflow.step.StepSpecDto
import de.lise.fluxflow.rest.workflow.step.StepStatusDto

@ExperimentalApi
data class DefaultStepDto(
    override val metadata: StepMetadataDto,
    override val spec: StepSpecDto,
    override val status: StepStatusDto
) : StepDto