package de.lise.fluxflow.springboot.web

import com.fasterxml.jackson.databind.ObjectMapper
import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException

class SpringWebRequestBodyParameterResolutionTest {
    private val objectMapper = ObjectMapper()

    @AfterEach
    fun resetRequestContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `should deserialize a valid request body`() {
        givenRequestBody("""{"name":"test"}""")

        val value = resolution(optional = false).get()

        assertThat(value).isInstanceOfSatisfying(TestPayload::class.java) {
            assertThat(it.name).isEqualTo("test")
        }
    }

    @Test
    fun `should return null for a missing body if the parameter is optional`() {
        givenRequestBody("")

        assertThat(resolution(optional = true).get()).isNull()
    }

    @Test
    fun `should respond with a bad request error for a missing body if the parameter is required`() {
        givenRequestBody("")

        assertThatThrownBy {
            resolution(optional = false).get()
        }.isInstanceOfSatisfying(ResponseStatusException::class.java) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Test
    fun `should respond with a bad request error for a malformed body even if the parameter is optional`() {
        givenRequestBody("{ this is not json")

        assertThatThrownBy {
            resolution(optional = true).get()
        }.isInstanceOfSatisfying(ResponseStatusException::class.java) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    private fun resolution(optional: Boolean): SpringWebRequestBodyParameterResolution {
        val function = this::stepAction
        return SpringWebRequestBodyParameterResolution(
            FunctionParameter(function, function.parameters.first()),
            optional,
            objectMapper,
            TestPayload::class.java
        )
    }

    private fun givenRequestBody(content: String) {
        val request = MockHttpServletRequest()
        request.setContent(content.toByteArray())
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    class TestPayload {
        var name: String? = null
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    fun stepAction(@RequestBody data: TestPayload) {
    }
}
