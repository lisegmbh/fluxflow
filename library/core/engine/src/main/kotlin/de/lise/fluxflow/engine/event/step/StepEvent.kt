package de.lise.fluxflow.engine.event.step

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.step.Step

/**
 * Base class for all events related to a [Step].
 */
abstract class StepEvent(val step: Step) : FlowEvent
