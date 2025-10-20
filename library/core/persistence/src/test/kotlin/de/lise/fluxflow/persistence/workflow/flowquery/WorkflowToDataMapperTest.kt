package de.lise.fluxflow.persistence.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.workflow.WorkflowData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WorkflowToDataMapperTest {
    @Test
    fun `mapping should produces the correct results`() {
        // Arrange
        val mapper = WorkflowToDataMapper()
        val testExpressions = mapOf(
            Expression.root<Workflow<Any>>()
                .get(Workflow<Any>::model)
                .isEqual("Test")
            to Expression.root<WorkflowData>()
                .get(WorkflowData::model)
                .isEqual("Test"),
            Expression.root<Workflow<Int>>()
                .get(Workflow<Int>::identifier)
                .isEqual(WorkflowIdentifier("test"))
            to Expression.root<WorkflowData>()
                .get(WorkflowData::id)
                .isEqual("test"),
            Expression.root<Workflow<Boolean>>()
                .get(Workflow<Boolean>::identifier)
                .get(WorkflowIdentifier::value)
                .isEqual("test")
            to Expression.root<WorkflowData>()
                .get(WorkflowData::id)
                .isEqual("test")
        )

        // Act
        val results = testExpressions.mapValues {
            mapper.mapOrKeep(it.key)
        }

        // Assert
        assertThat(results).containsExactlyEntriesOf(testExpressions)
    }
}