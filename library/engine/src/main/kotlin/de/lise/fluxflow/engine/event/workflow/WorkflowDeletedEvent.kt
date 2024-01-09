package de.lise.fluxflow.engine.event.workflow

import de.lise.fluxflow.api.workflow.Workflow
class WorkflowDeletedEvent(workflow: Workflow<*>) : WorkflowEvent(workflow)