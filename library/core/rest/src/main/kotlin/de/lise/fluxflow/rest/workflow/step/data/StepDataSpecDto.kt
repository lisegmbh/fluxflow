package de.lise.fluxflow.rest.workflow.step.data

import de.lise.fluxflow.rest.ResourceSpecDto

interface StepDataSpecDto : ResourceSpecDto {
    val value: Any?
}