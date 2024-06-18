package de.lise.fluxflow.stereotyped.unwrapping

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStepDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class UnwrapServiceImplTest {
    @Test
    fun `unwrap should return the backing instance when called with a reflected step definition`() {
        // Arrange
        val instance = Any()
        val statefulStepDefinition = ReflectedStatefulStepDefinition(
            instance,
            StepKind("test"),
            NoVersion(),
            emptyList(),
            emptyList(),
            emptyMap(),
            emptySet(),
            emptySet()
        )
        val step = mock<Step> {
            on { it.definition } doReturn statefulStepDefinition
        }
        val unwrapService = UnwrapServiceImpl()

        // Act
        val unwrappedInstance = unwrapService.unwrap<Any>(step)

        // Assert
        assertThat(unwrappedInstance).isSameAs(instance)
    }

    @Test
    fun `unwrap should throw an exception when called with another step definition`() {
        // Arrange
        val otherDefinition = mock<StepDefinition>{ }
        val step = mock<Step>{
            on { it.definition } doReturn otherDefinition
        }

        val unwrapService = UnwrapServiceImpl()

        // Act & Assert
        val exception = assertThrows<IllegalUnwrapException> { unwrapService.unwrap(step) }
        assertThat(exception.elementToBeUnwrapped).isSameAs(step)
    }
}