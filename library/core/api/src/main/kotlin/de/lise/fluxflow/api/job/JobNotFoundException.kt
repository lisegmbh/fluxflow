package de.lise.fluxflow.api.job

import de.lise.fluxflow.api.workflow.Workflow

class JobNotFoundException(
    val workflow: Workflow<*>,
    val identifier: JobIdentifier
) : Exception(
    "The job with id '$identifier' could not be found for workflow with id '${workflow.identifier}'"
)