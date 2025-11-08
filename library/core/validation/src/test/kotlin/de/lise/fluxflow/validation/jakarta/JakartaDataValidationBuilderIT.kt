package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.validation.PropertyValidationConstraint
import de.lise.fluxflow.api.step.stateful.data.validation.PropertyValidationConstraintImpl
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataListenerDefinitionBuilder
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class JakartaDataValidationBuilderIT {
    private val validator = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()
        .validator

    private val builder = JakartaDataValidationBuilder(validator)

    private val dataDefinitionBuilder = DataDefinitionBuilder(
        mock<DataListenerDefinitionBuilder> { },
        builder,
        mock<MetadataBuilder> { }
    )

    @Test
    fun `validation definitions should include nested validation constraint present on a property's type`() {
        // Act
        val validationDefinition = builder.buildValidations(
            DataKind("test"),
            TestModel::class,
            TestModel::testObject
        )!!

        // Assert
        assertThat(validationDefinition.constraints).hasOnlyElementsOfType(PropertyValidationConstraintImpl::class.java)
        val propertyConstraint = validationDefinition.constraints.first() as PropertyValidationConstraint
        assertThat(propertyConstraint.constraints).hasSize(NestedTestModel.NumberOfValidationsForSimpleTestProp)
    }

    @Test
    fun `validation definitions should include nested validation constraint present on a property's type for collection`() {
        // Act
        val validationDefinition = builder.buildValidations(
            DataKind("test"),
            TestModel::class,
            TestModel::testObjects
        )!!

        // Assert
        assertThat(validationDefinition.constraints).hasOnlyElementsOfType(PropertyValidationConstraintImpl::class.java)
        val propertyConstraint = validationDefinition.constraints.first() as PropertyValidationConstraint
        assertThat(propertyConstraint.constraints).hasSize(NestedTestModel.NumberOfValidationsForSimpleTestProp)
    }

    @Test
    fun `validation definitions should include recursive validation constraint present on nested properties`() {
        // Act
        val validationDefinition = builder.buildValidations(
            DataKind("test"),
            TestModelWithRecursiveProp::class,
            TestModelWithRecursiveProp::someProp
        )!!

        // Assert
        assertThat(validationDefinition.constraints).hasExactlyElementsOfTypes(PropertyValidationConstraintImpl::class.java)
        val propertyConstraint = validationDefinition.constraints.first() as PropertyValidationConstraint
        assertThat(propertyConstraint.constraints).hasSize(TypeWithRecursiveProp.NumberOfValidationsForRecursiveProp)
    }

    @Test
    fun `validation definitions should include constraints from imported data properties`() {
        // Act
        val dataDefinitions = dataDefinitionBuilder.buildDataDefinition(TestModelWithImportedData::class)

        // Assert
        val dataDefinition = dataDefinitions.single { it.kind == DataKind(ImportedData::someProperty.name) }
        val propertyConstraint = dataDefinition.validation?.constraints?.first()
        assertThat(propertyConstraint).isNotNull()
    }
}