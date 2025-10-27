package de.lise.fluxflow.mongo.step

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.query.filter.MongoEqualFilter
import de.lise.fluxflow.mongo.step.query.filter.StepDocumentFilter
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.query.StepDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.Page
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort

class StepMongoPersistenceTest {

    @Test
    fun `findForWorkflow appends the workflow id to the query`() {
        // Arrange
        val stepRepository = mock<StepRepository> {
            on { findAll(any<Query<StepDocumentFilter, Sort>>()) } doReturn (Page.unpaged(emptyList()))
        }
        val queryRepository = mock<MongoFlowQueryRepository<StepDocument>> {}
        val queryMapper = mock<QueryMapper<StepData, StepDocument>> {}
        val stepMongoPersistence = StepMongoPersistence(
            stepRepository,
            queryRepository,
            queryMapper
        )

        // Act
        stepMongoPersistence.findForWorkflow(
            WorkflowIdentifier("workflow-id-from-param"),
            StepDataQuery.empty()
        )


        // Assert
        val captor = argumentCaptor<Query<StepDocumentFilter, Sort>>()
        verify(stepRepository, times(1)).findAll(captor.capture())

        val executedQuery = captor.firstValue
        assertThat(executedQuery.filter?.workflowId).isEqualTo((MongoEqualFilter("workflow-id-from-param")))
    }
}