package de.lise.fluxflow.rest.workflow.step

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.rest.ResourceStatusDto

interface StepStatusDto : ResourceStatusDto {
    val status: Status
}