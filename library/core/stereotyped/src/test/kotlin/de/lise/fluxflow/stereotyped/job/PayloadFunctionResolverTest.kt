package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.reflection.activation.parameter.ParameterProvider
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@Suppress("UNUSED_PARAMETER")
class PayloadFunctionResolverTest {
    @Test
    fun `resolve should throw an exception if a param could not be resolved`() {
        // Arrange
        val parameterResolver = mock<ParameterResolver> {}
        val instanceProvider = mock<ParameterProvider<TestInstance>> {}
        val jobProvider = mock<ParameterProvider<Job>> {}
        val payloadFunctionResolver = PayloadFunctionResolver(
            parameterResolver,
            TestInstance::payloadWithUnresolvableParam,
            instanceProvider,
            jobProvider
        )

        // Act
        assertThrows<JobConfigurationException> {
            payloadFunctionResolver.resolve()
        }
    }

    @Test
    fun `resolve should return a resolution that uses the given instance upon invocation`() {
        // Arrange
        val parameterResolver = mock<ParameterResolver> {}
        val testInstance = TestInstance()
        val instanceProvider = mock<ParameterProvider<TestInstance>> {
            on { provide() }.doReturn(testInstance)
        }
        val jobProvider = mock<ParameterProvider<Job>> {}
        val payloadFunctionResolver = PayloadFunctionResolver(
            parameterResolver,
            TestInstance::payloadWithoutParams,
            instanceProvider,
            jobProvider
        )

        // Act
        val result = payloadFunctionResolver.resolve()
        result.call()

        // Assert
        assertThat(testInstance.invoked).isTrue
    }

    @Test
    fun `resolve should return a resolution that resolves job parameters using the current job`() {
        // Arrange
        val parameterResolver = mock<ParameterResolver> {}
        val testInstance = TestInstance()
        val instanceProvider = mock<ParameterProvider<TestInstance>> {
            on { provide() }.doReturn(testInstance)
        }
        val testJob = mock<Job> {}
        val jobProvider = mock<ParameterProvider<Job>> {
            on { provide() }.doReturn(testJob)
        }
        val payloadFunctionResolver = PayloadFunctionResolver(
            parameterResolver,
            TestInstance::payloadWithJob,
            instanceProvider,
            jobProvider
        )

        // Act
        val result = payloadFunctionResolver.resolve()
        result.call()

        // Assert
        assertThat(testInstance.invoked).isTrue
        assertThat(testInstance.receivedJob).isSameAs(testJob)
    }

    @Test
    fun `resolve should try to resolve parameters that are not the current instance or a job using the parent resolver`() {
        // Arrange
        val testParam = TestParam()
        val testParamResolution = mock<ParameterResolution> {
            on { get() }.doReturn(testParam)
        }
        val parameterResolver = mock<ParameterResolver> {
            on { resolveParameter(any()) }.doReturn(testParamResolution)
        }
        val testInstance = TestInstance()
        val instanceProvider = mock<ParameterProvider<TestInstance>> {
            on { provide() }.doReturn(testInstance)
        }
        val jobProvider = mock<ParameterProvider<Job>> {}
        val payloadFunctionResolver = PayloadFunctionResolver(
            parameterResolver,
            TestInstance::payloadWithResolvableParam,
            instanceProvider,
            jobProvider
        )

        // Act
        val result = payloadFunctionResolver.resolve()
        result.call()

        // Assert
        assertThat(testInstance.receivedParam).isSameAs(testParam)
    }

    class TestParam
    class TestInstance{
        var invoked: Boolean = false
        var receivedJob: Job? = null
        var receivedParam: TestParam? = null

        fun payloadWithoutParams() {
            invoked = true
        }
        fun payloadWithJob(job: Job) {
            invoked = true
            receivedJob = job
        }
        fun payloadWithResolvableParam(param: TestParam) {
            invoked = true
            receivedParam = param
        }
        fun payloadWithUnresolvableParam(unresolvableStringParam: String) {}
    }
}