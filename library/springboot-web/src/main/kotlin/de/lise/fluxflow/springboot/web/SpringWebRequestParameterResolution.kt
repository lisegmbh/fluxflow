package de.lise.fluxflow.springboot.web

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.TypeConverter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException

class SpringWebRequestParameterResolution(
    parameter: FunctionParameter<*>,
    private val requestParam: RequestParam,
    private val targetType: Class<*>,
    private val typeConverter: TypeConverter,
    private val paramNames: List<String>
) : SpringWebRequestContextResolution(parameter) {
    override fun get(request: HttpServletRequest): Any? {
        val paramValues = paramNames.firstNotNullOfOrNull {
            request.getParameterValues(it)
        }

        if(paramValues == null && requestParam.required) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: ${paramNames.first()}")
        }

        if(paramValues == null) {
            return null
        }

        return typeConverter.convertIfNecessary(paramValues, targetType)
    }
}