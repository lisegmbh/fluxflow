package de.lise.fluxflow.springboot.activation.parameter

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanExpressionContext
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import kotlin.reflect.KClass

class SpringValueExpressionParameterResolver(
    private val beanFactory: ConfigurableBeanFactory,
) : ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        val valueExpression = functionParam.param.annotations.firstNotNullOfOrNull { (it as? Value)?.value } ?: return null
        
        return ParameterResolution { 
            beanFactory.typeConverter.convertIfNecessary(
                beanFactory.beanExpressionResolver!!.evaluate(
                     beanFactory.resolveEmbeddedValue(valueExpression),
                    BeanExpressionContext(beanFactory, null)
                ),
                (functionParam.param.type.classifier as KClass<*>).java
            )
        }
    }
}