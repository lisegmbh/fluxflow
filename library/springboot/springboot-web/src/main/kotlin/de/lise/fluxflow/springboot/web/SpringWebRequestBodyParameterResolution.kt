package de.lise.fluxflow.springboot.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class SpringWebRequestBodyParameterResolution(
    functionParam: FunctionParameter<*>,
    private val optional: Boolean,
    private val objectMapper: ObjectMapper,
    private val targetType: Class<*>
) : SpringWebRequestContextResolution(functionParam) {
    override fun get(request: HttpServletRequest): Any? {
        return try {
            objectMapper.readValue(request.inputStream, targetType)
        } catch (e: MismatchedInputException) {
            if(!optional) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing or invalid request body. A ${targetType.simpleName} is required."
                )
            }
            null
        }
    }
}