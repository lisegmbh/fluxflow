package de.lise.fluxflow.api.validation

/**
 * Controls if the step validation must succeed in order for this continuation to run.
 */
enum class ValidationBehavior {
    /**
     * The default behavior requires steps to be valid, whenever it is about to be completed.
     */
    Default,

    /**
     * If this behavior is specified and the step fails validation,
     * the normal execution is aborted by throwing an exception.
     */
    OnlyValid,

    /**
     * If this behavior is specified, step validation will be skipped.
     */
    AllowInvalid
}