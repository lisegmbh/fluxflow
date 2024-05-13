package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.validation.PropertyValidationConstraint
import de.lise.fluxflow.api.step.stateful.data.validation.PropertyValidationConstraintImpl
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.junit.jupiter.api.Test

class JakartaDataValidationBuilderTest {
    @Test
    fun `validation definitions should include recursive validation constraint present on nested properties`() {
        // Arrange
        val validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(ParameterMessageInterpolator())
            .buildValidatorFactory()
            .validator;
        
        val builder = JakartaDataValidationBuilder(validator)

        // Act
        val validationDefinition = builder.buildValidations(
            DataKind("test"),
            TestModel::class,
            TestModel::testObject
        )!!.build(TestModel())

        // Assert
        assertThat(validationDefinition.constraints).hasOnlyElementsOfType(PropertyValidationConstraintImpl::class.java)
        val propertyConstraint = validationDefinition.constraints.first() as PropertyValidationConstraint
        assertThat(propertyConstraint.constraints).hasSize(NestedTestModel.NumberOfValidationsForSimpleTestProp)
    }
}