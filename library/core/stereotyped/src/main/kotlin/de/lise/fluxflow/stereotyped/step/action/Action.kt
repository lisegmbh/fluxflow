package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

/**
 * Step methods can be annotated with [Action] to indicate, that these methods can be invoked by the workflow engine
 * to trigger next steps or other actions.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Action(
    /**
     * By default, the method's name is used as an identifier.
     * This attribute can be used to overwrite the default name.
     */
    val value: String = "",
    /**
     * Defines how the originating step's status should be updated after the action ran successfully.
     * For more information on the available modes, refer to the documentation on [ImplicitStatusBehavior].
     * @see ImplicitStatusBehavior
     */
    val statusBehavior: ImplicitStatusBehavior = ImplicitStatusBehavior.Default,
    /**
     * Defines the validation behavior that should be applied to the step before running the annotated action.
     */
    val beforeExecutionValidation: ValidationBehavior = ValidationBehavior.Default,

    /**
     * Defines the validation groups to be evaluated before invoking this action.
     */
    val validationGroups: Array<KClass<*>> = []
)
