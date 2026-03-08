package de.lise.fluxflow.springboot.rest.workflow.step.data

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.rest.workflow.step.data.StepDataDto
import de.lise.fluxflow.rest.workflow.step.data.StepDataMetadataDto
import de.lise.fluxflow.rest.workflow.step.data.StepDataSpecDto

@ExperimentalApi
class DefaultStepDataDto(
    override val metadata: StepDataMetadataDto,
    override val spec: StepDataSpecDto
) : StepDataDto {
    override val kind: String
        get() = "stepData"
}