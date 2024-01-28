FluxFlow allows data validation using the standard Jakarta Bean
Validation (formerly `javax.validation`).

# Getting started

Validation must be enabled separately by providing an implementation for
the `jakarta.validation.Validator`.

## Adding a Bean Validation implementation

If you don’t already have a compatible implementation on your class
path, you need to add it. In order to do so, you would usually declare
the desired library as a build dependency. This documentation will be
using the [Hibernate Validator](https://hibernate.org/validator/) as an
example, but you can use any Jakarta Bean Validation compatible
implementation (e.g. [Apache BVal](https://bval.apache.org)).

If you are using Spring JPA, the [Hibernate
Validator](https://hibernate.org/validator/) should already be
available.

**Adding Hibernate Validator within your build.gradle**

    implementation("org.hibernate.validator:hibernate-validator:<CURRENT VERSION>") // 

-   Be sure to set the desired
    [version](https://mvnrepository.com/artifact/org.hibernate.validator/hibernate-validator)

## Register the validator

In order for FluxFlow to pick up the Bean Validator, you need to
register it to the dependency injection container. When using Spring,
this can be archived by adding the `LocalValidatorFactoryBean` bean
within a `@Configuration`.

**Registering the validator**

    import jakarta.validation.Validator
    import org.springframework.context.annotation.Bean
    import org.springframework.context.annotation.Configuration
    import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

    @Configuration
    class ValidationConfiguration {
        @Bean
        fun validator(): Validator {
            return LocalValidatorFactoryBean()
        }
    }

# Adding validation constraints

Validation is currently supported for step data. To define new
validation constraints, the regular Jakarta annotations are used. Simply
add them to your data definitions and you are all set.

**Example of different validation annotations**

    @Step
    class SubmitContactFormStep(
        @field:NotBlank // 
        @Data
        var firstname: String = "",
        @get:NotBlank
        @Data
        var lastname: String = ""
    ) {

        private var _acceptedTermsAndConditions: Boolean = false

        @get:AssertTrue // 
        var acceptTermsAndConditions: Boolean
            get() {
                return _acceptedTermsAndConditions
            }
            set(value) {
                _acceptedTermsAndConditions = true
            }

        // ...
    }

-   Can be applied to properties (as long as they have a backing field).

-   If they have custom getter/setters, you must explicitly apply them
    to the property’s getter.

Validation annotations are currently only supported on fields and
getters. If you are using Kotlin’s primary constructor properties, you
must therefore prefix the annotation with `@field:` or `@get:`. Failing
to do so will cause a `ValidationConfigurationException` to be thrown.

If complex step data types (e.g., objects, lists, maps) are used, the
`@Valid` annotation must be applied to the step data property.

# Executing validation

During a workflow’s execution, there are two times when validation rules
can be evaluated.

1.  Before a step action is invoked.

2.  After a step action ran.

You may control the actual behavior using global settings, the `@Action`
annotation or the returned continuation.

## Default validation behavior

By default, FluxFlow will apply the following validation behavior.

1.  Validation is always executed before invoking a step action.

2.  Validation is done a second time, if the action returns a
    continuation that is going to complete the step.

These defaults make sure that an action does not operate on invalid data
and does not itself invalidate them right before a step will become
completed.

## Skip default validation before actions

You can set the `fluxflow.action.validate-before` application setting to
`false` (e.g. within your application.yml). This will effectively
disable the validation that is done before an action’s execution.

If you do not specify this setting yourself, `true` will be assumed.

The global setting will not affect actions, which explicitly specify a
custom validation behavior.

## Skip or force validation before specific actions

The validation that is done before an action is invoked, can also be set
explicitly using the `@Action` annotation.

**Control if validation should be performed before an action is
invoked**

    @Step
    class SubmitContactFormStep(
        @get:NotBlank
        @Data
        var firstname: String = "",
        // ...
    ) {

        @Action(beforeExecutionValidation = ValidationBehavior.AllowInvalid) // 
        fun completeAndFix() {
            if(firstname.isBlank()) {
                firstname = "<Placeholder>"
            }
            // ...
        }

        @Action(beforeExecutionValidation = ValidationBehavior.OnlyValid) // 
        fun completeStrictly() {
            // ...
        }

        @Action(beforeExecutionValidation = ValidationBehavior.Default) // 
        fun complete() {
            // ...
        }

    }

-   This will skip the pre-execution validation, which allows the action
    to fix the offending data. As the action is going to complete the
    current step, validation will be run after the action ran.

-   This will force the pre-execution validation to be evaluated -
    regardless of the [global
    setting](#validation_execution_global_setting).

-   Use the default behavior, which is controlled by the [global
    setting](#validation_execution_global_setting).

## Skip or force validation after specific actions

Similar to the [pre-execution validation](#validation_execution_before),
post-execution behavior can be customized as well. This is done by
applying a custom validation behavior to the returned continuation
(calling `.withValidationBehavior(...)`).

**Control if validation should be performed after an action ran**

    @Step
    class SubmitContactFormStep(
        @get:NotBlank
        @Data
        var firstname: String = "",
        // ...
    ) {

        @Action
        fun completeAnyway(): Continuation<*> {
            firstname = ""
            return Continuation.none()
                .withValidationBehavior(ValidationBehavior.AllowInvalid) // 
        }

        @Action
        fun completeStrictly(): Continuation<*> {
            firstname = ""
            return Continuation.none()
                .withValidationBehavior(ValidationBehavior.OnlyValid) // 
        }

        @Action
        fun completeMaybe(): Continuation<*> {
            firstname = ""
            return Continuation.none()
                .withValidationBehavior(ValidationBehavior.Default) // 
        }

    }

-   This will allow the workflow to continue, even if the firstname is
    obviously invalid.

-   This will cause a validation error to be thrown, as the firstname
    will be obviously blank.

-   If validation is performed, will depend on whenever the action will
    complete the current step. As the step within the example does so, a
    validation error is going to be thrown.

# Catching validation errors

Whenever a validation fails, a `DataValidationException` will be thrown.
You may catch this exception and consult the `issues` property regarding
further details.
