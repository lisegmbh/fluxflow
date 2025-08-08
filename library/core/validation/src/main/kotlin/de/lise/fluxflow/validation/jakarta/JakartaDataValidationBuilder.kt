package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationConstraint
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import de.lise.fluxflow.api.step.stateful.data.validation.PropertyValidationConstraintImpl
import de.lise.fluxflow.api.step.stateful.data.validation.ValidationConfigurationException
import de.lise.fluxflow.reflection.property.findAnnotationEverywhere
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import jakarta.validation.Constraint
import jakarta.validation.Valid
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

class JakartaDataValidationBuilder(
    private val validator: Validator,
) : ValidationBuilder {
    private val validatedTypes = mutableSetOf<KClass<*>>()

    override fun <TInstance : Any, TProp : Any?> buildValidations(
        dataKind: DataKind,
        instanceType: KClass<out TInstance>,
        prop: KProperty1<TInstance, TProp>,
    ): DataValidationDefinition? {
        checkValidationConfiguration(instanceType)
        val propertyName = prop.name

        val allConstraints = buildValidations(
            instanceType,
            prop,
            mutableMapOf()
        )
        if (allConstraints.isEmpty()) {
            return null
        }

        return JakartaValidationDefinition(
            allConstraints,
            dataKind,
            validator,
            propertyName
        )
    }

    private fun <TInstance : Any, TProp : Any?> buildValidations(
        instanceType: KClass<out TInstance>,
        prop: KProperty1<out TInstance, TProp>,
        recursionCache: MutableMap<KProperty1<*, *>, List<DataValidationConstraint>>,
    ): List<DataValidationConstraint> {
        val recursiveResult = recursionCache[prop]
        if (recursiveResult != null) {
            return recursiveResult
        }
        val result = mutableListOf<DataValidationConstraint>()
        recursionCache[prop] = result

        val propertyName = prop.name
        Logger.debug(
            "Building validation for {}.{}",
            instanceType::qualifiedName,
            propertyName
        )
        val allConstraints = validator.getConstraintsForClass(instanceType.java)
        val propertyConstraints = allConstraints.getConstraintsForProperty(propertyName) ?: return emptyList()
        result += propertyConstraints.constraintDescriptors.map {
            Logger.debug(
                "Found validation constraint \"{}\" for {}.{}",
                it,
                instanceType::qualifiedName,
                propertyName
            )
            JakartaDataValidationConstraint(
                it.annotation?.annotationClass?.simpleName,
                it.attributes
            )
        }

        prop.findAnnotationEverywhere<Valid>() ?: return result

        val propTypeClassifier = prop.returnType.classifier as KClass<*>? ?: return result

        val propertyType: KClass<*> = if (isCollectionType(propTypeClassifier)) {
            // We need to get the type of the collection elements and not the collection itself
            prop.returnType.arguments.first().type!!.classifier as? KClass<*> ?: return result
        } else {
            propTypeClassifier
        }

        result += propertyType
            .memberProperties
            .associateWith { nestedProp ->
                buildValidations(
                    propertyType,
                    nestedProp,
                    recursionCache
                )
            }
            .filter {
                it.value.isNotEmpty()
            }
            .map {
                PropertyValidationConstraintImpl(
                    Valid::class.simpleName,
                    emptyMap(),
                    it.key.name,
                    it.value
                )
            }
        return result
    }

    private fun checkValidationConfiguration(type: KClass<*>) {
        if (validatedTypes.contains(type)) {
            // The type has already been checked
            return
        }

        type.constructors.forEach { constructor ->
            constructor.parameters.map { param ->
                val invalidAnnotations = param.annotations.filter { annotation ->
                    // Check if the annotation itself is annotated with @Constraint
                    annotation.annotationClass.hasAnnotation<Constraint>()
                }
                if (invalidAnnotations.isNotEmpty()) {
                    throw ValidationConfigurationException(
                        "The parameter \"${param.name}\" of ${type.qualifiedName}'s constructor must not be annotated with validation annotations (${
                            invalidAnnotations.joinToString(", ") { annotation -> "@${annotation.annotationClass.simpleName}" }
                        }). If you are using Kotlin, you might want to apply them to the field or getter. For example using:\n${
                            invalidAnnotations.joinToString("\n") { annotation ->
                                "- @field:@${annotation.annotationClass.simpleName} or @get:@${annotation.annotationClass.simpleName}"
                            }
                        })"
                    )
                }
            }
        }

        validatedTypes.add(type)
    }

    private fun isCollectionType(classifier: KClassifier): Boolean {
        val collectionTypes = setOf(
            Collection::class,
            List::class,
            Set::class,
            Map::class,
            Array::class
        )

        return classifier in collectionTypes
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(JakartaDataValidationBuilder::class.java)!!
    }
}