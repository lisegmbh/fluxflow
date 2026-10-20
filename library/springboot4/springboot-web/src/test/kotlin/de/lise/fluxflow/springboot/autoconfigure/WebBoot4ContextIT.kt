package de.lise.fluxflow.springboot.autoconfigure

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.springboot.web.SpringWebParameterResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@SpringBootTest(classes = [WebBoot4ContextIT.WebTestConfiguration::class])
class WebBoot4ContextIT {

    @Autowired
    lateinit var springWebParameterResolver: ParameterResolver

    @AfterEach
    fun resetRequestContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `should load web parameter resolver on Spring Boot 4`() {
        assertThat(springWebParameterResolver).isInstanceOf(SpringWebParameterResolver::class.java)
    }

    @Test
    fun `should deserialize request bodies into Kotlin data classes using the auto-configured Jackson 3 mapper`() {
        val function = this::stepAction
        val resolution = springWebParameterResolver.resolveParameter(
            FunctionParameter(function, function.parameters.first())
        )

        assertThat(resolution).isNotNull

        val request = MockHttpServletRequest()
        request.setContent("""{"name":"test","count":42}""".toByteArray())
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        assertThat(resolution!!.get()).isEqualTo(TestStepData("test", 42))
    }

    data class TestStepData(val name: String, val count: Int)

    @Suppress("UNUSED_PARAMETER", "unused")
    fun stepAction(@RequestBody data: TestStepData) {
    }

    @Configuration
    @ImportAutoConfiguration(JacksonAutoConfiguration::class)
    @Import(SpringWebSupportConfiguration::class)
    open class WebTestConfiguration
}
