package de.lise.fluxflow.api.step.stateful.action

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

interface ActionDefinition {
    val kind: ActionKind
    /**
     * A simple key-value map providing additional metadata about this action definitions and actions executed based on it.
     * 
     * This property is never `null` and does not contain `null` values.
     * The map will be empty if there is no metadata.
     */
    val metadata: Map<String, Any>

    /**
     * Specifies the validation behavior that should be applied to the step before running the defined action.
     */
    val beforeExecutionValidation: ValidationBehavior

    /**
     * Specifies which validation groups to be executed before invoking this action.
     */
    val validationGroups: Set<KClass<*>>
    
    fun createAction(step: Step): Action
}