package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.rest.workflow.step.StepStatusDto

data class DefaultStepStatusDto(
    override val status: Status
) : StepStatusDto