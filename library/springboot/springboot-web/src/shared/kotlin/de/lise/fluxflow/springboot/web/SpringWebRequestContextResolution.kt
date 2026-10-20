package de.lise.fluxflow.springboot.web

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

abstract class SpringWebRequestContextResolution(
    private val parameter: FunctionParameter<*>
) : ParameterResolution {

    override fun get(): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw IllegalStateException(
                "Can not resolve param \"${parameter.param.name}\" of function \"${parameter.function.name}\" outside of an http request context."
            )
        return get(request)
    }

    protected abstract fun get(request: HttpServletRequest): Any?
}