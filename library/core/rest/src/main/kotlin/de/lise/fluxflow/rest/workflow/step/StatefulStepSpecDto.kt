package de.lise.fluxflow.rest.workflow.step

interface StatefulStepSpecDto : StepSpecDto {
    val data: Map<String, Any?>
}