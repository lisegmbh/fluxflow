package de.lise.fluxflow.engine.event.workflow

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.workflow.Workflow

/**
 * Base class for all workflow related events.
 */
abstract class WorkflowEvent(override val workflow: Workflow<*>) : FlowEvent