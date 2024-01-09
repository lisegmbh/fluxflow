package de.lise.fluxflow.stereotyped.job.parameter

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.parameter.Parameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class ParameterDefinitionBuilderTest {
    @Test
    fun `build should return a builder that creates a data definition accessing the correct property`() {
        // Arrange
        val instance = TestJob("test value")
        val parameter = createParameterObject(
            instance,
            TestJob::readOnlyProperty
        )
        
        // Act
        val value = parameter.get()
        
        // Assert
        assertThat(value).isEqualTo("test value")        
    }
    
    @Test
    fun `isParameterProperty should return true for public properties, that do not represent a job`() {
        // Arrange
        val parameterDefinitionBuilder = ParameterDefinitionBuilder()
        
        // Act
        val result = parameterDefinitionBuilder.isParameterProperty(TestJob::readOnlyProperty)
        
        // Assert
        assertThat(result).isTrue
    }
    
    @Test
    fun `isParameterProperty should return false for private properties, even if they do not represent a job`() {
        // Arrange
        val parameterDefinitionBuilder = ParameterDefinitionBuilder()

        // Act
        val result = parameterDefinitionBuilder.isParameterProperty(
            TestJobWithDifferentProps::class.memberProperties.first { it.name == "somePrivateProperty" }
        )

        // Assert
        assertThat(result).isFalse
    }
    
    @Test
    fun `isParameterProperty should return false for public properties, that do represent a job`() {
        // Arrange
        val parameterDefinitionBuilder = ParameterDefinitionBuilder()

        // Act
        val result = parameterDefinitionBuilder.isParameterProperty(
            TestJobWithDifferentProps::testJobProperty
        )

        // Assert
        assertThat(result).isFalse
    }

    private fun <TObject : Any, TProp> createParameterObject(
        instance: TObject,
        prop: KProperty1<out TObject, TProp>
    ): Parameter<TProp> {
        val parameterDefinitionBuilder = ParameterDefinitionBuilder()
        val job = mock<Job> {}

        val builder = parameterDefinitionBuilder.buildParameterDefinitionFromProperty(
            prop
        )
        @Suppress("UNCHECKED_CAST")
        return builder(instance).createParameter(job) as Parameter<TProp>
    }
    
    @de.lise.fluxflow.stereotyped.job.Job
    class TestJob(
        val readOnlyProperty: String
    )
    class TestJobWithDifferentProps(
        private val somePrivateProperty: String,
        val testJobProperty: TestJob
    )
}