package de.lise.fluxflow.rest.workflow

import de.lise.fluxflow.rest.ResourceSpecDto

interface WorkflowSpecDto : ResourceSpecDto {
    val model: Any?
}