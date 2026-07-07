package de.lise.fluxflow.springboot.web

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import tools.jackson.core.JacksonException
import tools.jackson.core.exc.JacksonIOException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.MismatchedInputException

class SpringWebRequestBodyParameterResolution(
    functionParam: FunctionParameter<*>,
    private val optional: Boolean,
    private val objectMapper: ObjectMapper,
    private val targetType: Class<*>
) : SpringWebRequestContextResolution(functionParam) {
    override fun get(request: HttpServletRequest): Any? {
        return try {
            objectMapper.readValue(request.inputStream, targetType)
        } catch (e: JacksonIOException) {
            // Underlying I/O failures are not client errors and must not become a 400.
            throw e
        } catch (e: JacksonException) {
            if(optional && e is MismatchedInputException) {
                return null
            }
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Missing or invalid request body. A ${targetType.simpleName} is required."
            )
        }
    }
}
