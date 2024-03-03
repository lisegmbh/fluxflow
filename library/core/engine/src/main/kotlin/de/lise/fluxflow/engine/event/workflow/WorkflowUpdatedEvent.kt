package de.lise.fluxflow.engine.event.workflow

import de.lise.fluxflow.api.workflow.Workflow

class WorkflowUpdatedEvent(workflow: Workflow<*>) : WorkflowEvent(workflow)
