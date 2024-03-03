package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ModelListenerDefinitionBuilderIT {
    @Test
    fun `build should return a definition if there is one`() {
        val parameterResolver = mock<ParameterResolver> {}
        val expressionParser = mock<SelectorExpressionParser> {
            on { parse(any()) } doReturn SelectorExpression{ _, _, _ -> true }
        }
        val builder = ModelListenerDefinitionBuilder(
            parameterResolver,
            { old, new -> old != new },
            expressionParser
        )
        val model = TestModelWithListener()

        // Act
        val definitions = builder.build(model)

        // Assert
        assertThat(definitions).hasSize(1)
    }

    @Test
    fun `build should return a definition that's listener receives all provided values`() {
        val parameterResolver = mock<ParameterResolver> {}
        val expressionParser = mock<SelectorExpressionParser> {
            on { parse(any()) } doReturn SelectorExpression{ _, _, _ -> true }
        }
        val builder = ModelListenerDefinitionBuilder(
            parameterResolver,
            { old, new -> old != new },
            expressionParser
        )
        val oldModel = TestModelWithListener()
        val currentModel = TestModelWithListener()
        val workflow = mock<Workflow<TestModelWithListener>> {}

        // Act
        val definition = builder.build(currentModel).first()
        val listener = definition.create(workflow)
        listener.onChange(oldModel, currentModel)

        // Assert
        assertThat(currentModel.newValue).isSameAs(currentModel)
        assertThat(currentModel.oldValue).isSameAs(oldModel)
        assertThat(currentModel.workflow).isSameAs(workflow)
    }

    @Test
    fun `build should not return a definition if there is none`() {
        val parameterResolver = mock<ParameterResolver> {}
        val expressionParser = mock<SelectorExpressionParser> {
            on { parse(any()) } doReturn SelectorExpression { _, _, _ -> true }
        }
        val builder = ModelListenerDefinitionBuilder(
            parameterResolver,
            { old, new -> old != new },
            expressionParser
        )
        val model = TestModelWithoutListener()

        // Act
        val definitions = builder.build(model)

        // Assert
        assertThat(definitions).isEmpty()
    }

    class TestModelWithListener {

        var workflow: Workflow<TestModelWithListener>? = null
        var oldValue: TestModelWithListener? = null
        var newValue: TestModelWithListener? = null

        @ModelListener
        fun hasChanged(
            workflow: Workflow<TestModelWithListener>,
            oldValue: TestModelWithListener,
            newValue: TestModelWithListener
        ) {
            this.workflow = workflow
            this.oldValue = oldValue
            this.newValue = newValue
        }
    }

    class TestModelWithoutListener
}