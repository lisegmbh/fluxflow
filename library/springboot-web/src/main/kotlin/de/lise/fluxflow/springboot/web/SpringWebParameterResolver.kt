package de.lise.fluxflow.springboot.web

import com.fasterxml.jackson.databind.ObjectMapper
import de.lise.fluxflow.reflection.ReflectionUtils
import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolutionException
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import org.springframework.beans.TypeConverter
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import kotlin.reflect.full.findAnnotation


class SpringWebParameterResolver(
    private val typeConverter: TypeConverter,
    private val objectMapper: ObjectMapper
) : ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        val paramType = ReflectionUtils.getParameterClass(functionParam.param)?.java ?: return null
        val body = functionParam.param.findAnnotation<RequestBody>()
        if (body != null) {
            return SpringWebRequestBodyParameterResolution(
                functionParam,
                !body.required,
                objectMapper,
                paramType
            )
        }


        val param = functionParam.param.findAnnotation<RequestParam>()
        if (param != null) {
            val paramNames = listOfNotNull(
                param.name,
                param.value,
                functionParam.param.name
            ).filter {
                it.isNotBlank()
            }.distinct()

            if(paramNames.isEmpty()) {
                throw ParameterResolutionException(
                    functionParam,
                    "Could not determine the request parameter name."
                )
            }

            return SpringWebRequestParameterResolution(
                functionParam,
                param,
                paramType,
                typeConverter,
                paramNames
            )
        }

        return null
    }
}