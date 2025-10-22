package de.lise.fluxflow.persistence.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.flowquery.WorkflowQueryable
import de.lise.fluxflow.persistence.workflow.WorkflowData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WorkflowQueryableToDataMapperTest {
    @Test
    fun `mapping should produces the correct results`() {
        // Arrange
        val mapper = WorkflowQueryableToDataMapper()
        val testExpressions = mapOf(
            Expression.root<WorkflowQueryable<Any>>()
                .get(WorkflowQueryable<Any>::model)
                .isEqual("Test")
            to Expression.root<WorkflowData>()
                .get(WorkflowData::model)
                .isEqual("Test"),
            Expression.root<WorkflowQueryable<Int>>()
                .get(WorkflowQueryable<Int>::identifier)
                .isEqual(WorkflowIdentifier("test"))
            to Expression.root<WorkflowData>()
                .get(WorkflowData::id)
                .isEqual("test"),
            Expression.root<WorkflowQueryable<Boolean>>()
                .get(WorkflowQueryable<Boolean>::identifier)
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