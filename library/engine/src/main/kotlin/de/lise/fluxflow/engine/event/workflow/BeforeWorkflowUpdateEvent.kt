package de.lise.fluxflow.engine.event.workflow

import de.lise.fluxflow.api.workflow.Workflow

class BeforeWorkflowUpdateEvent(workflow: Workflow<*>): WorkflowEvent(workflow)