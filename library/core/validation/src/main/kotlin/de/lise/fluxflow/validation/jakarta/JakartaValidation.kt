package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidation
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationIssue
import jakarta.validation.Validator
import kotlin.reflect.KClass

class JakartaValidation<TInstance>(
    private val dataKind: DataKind,
    private val validator: Validator,
    private val instance: TInstance,
    private val propertyName: String
) : DataValidation {
    override fun validate(
        groups: Set<KClass<*>>
    ): Collection<DataValidationIssue> {
        // TODO: Issue #1 Avoid validating the entire object for each defined step data
        val groupArray = groups.map { it.java }.toTypedArray()
        val relevantValidationIssues = validator.validate(instance, *groupArray)
            .filter { violation ->
                violation.propertyPath.firstOrNull()?.name == propertyName
            }
        
        return relevantValidationIssues.map { violation ->

            violation.constraintDescriptor.attributes
                DataValidationIssue(
                    dataKind,
                    JakartaDataValidationConstraint(
                        violation.constraintDescriptor.annotation?.annotationClass?.simpleName,
                        violation.constraintDescriptor.attributes
                    )
                    ,
                    violation.propertyPath.toString(),
                    violation.message,
                    violation.invalidValue
                )

        }
    }
}