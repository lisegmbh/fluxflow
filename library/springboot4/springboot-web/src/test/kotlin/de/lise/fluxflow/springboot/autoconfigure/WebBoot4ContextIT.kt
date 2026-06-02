package de.lise.fluxflow.springboot.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.springboot.web.SpringWebParameterResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@SpringBootTest(classes = [WebBoot4ContextIT.WebTestConfiguration::class])
class WebBoot4ContextIT {

    @Autowired
    lateinit var springWebParameterResolver: ParameterResolver

    @Test
    fun `should load web parameter resolver on Spring Boot 4`() {
        assertThat(springWebParameterResolver).isInstanceOf(SpringWebParameterResolver::class.java)
    }

    @Configuration
    @Import(SpringWebSupportConfiguration::class)
    open class WebTestConfiguration {
        @Bean
        open fun objectMapper(): ObjectMapper = ObjectMapper()
    }
}
