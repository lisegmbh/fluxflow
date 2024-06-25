package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.step.stateful.data.DataDefinition

class StepDataAssignmentException(
    value: Any?,
    definition: DataDefinition<*>,
    step: Step,
    cause: Exception
) : StepActivationException("Failed setting value '$value' for data of kind '${definition.kind}' for step '${step.identifier}' of kind '${step.definition.kind}'", cause)
