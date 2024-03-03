package de.lise.fluxflow.engine.event.workflow

import de.lise.fluxflow.api.workflow.Workflow

class WorkflowCreatedEvent(workflow: Workflow<*>) : WorkflowEvent(workflow)
