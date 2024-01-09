package de.lise.fluxflow.api.workflow

class WorkflowNotFoundException(
    val identifier: WorkflowIdentifier
) : Exception("Workflow not found: '$identifier'")
