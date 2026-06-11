package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PredicateExpression
import de.fluxflow.flowquery.expression.compilation.CompilationResult
import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.query.sorting.Sort
import de.fluxflow.flowquery.query.sorting.SortDirection
import de.fluxflow.flowquery.query.sorting.Sorting
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.ConstantToken
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.ExpressionToken
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.StatementToken
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.aggregation.Aggregation

/**
 * Unit tests for [MongoQueryTranslator].
 *
 * These tests verify that query operations are correctly converted into MongoDB
 * aggregation stages using a mocked [MongoCompiler].
 */
class MongoQueryTranslatorTest {

    private lateinit var compiler: MongoCompiler
    private lateinit var translator: MongoQueryTranslator

    @BeforeEach
    fun setup() {
        compiler = mock()
        translator = MongoQueryTranslator(compiler)
    }

    // --------------------------------------------------------------------------------------------
    // Helper
    // --------------------------------------------------------------------------------------------

    private fun stagesOf(aggregation: Aggregation): List<Document> {
        return aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT)
    }

    // --------------------------------------------------------------------------------------------
    // Tests
    // --------------------------------------------------------------------------------------------

    @Test
    fun `should translate FilterOperation with ExpressionToken into match stage`() {
        // Arrange
        val predicate: PredicateExpression<Any> = Expression.root<Any>().isEqual(true)
        val operation = FilterOperation(predicate)

        val expressionToken = mock<ExpressionToken> {
            on { toExpression() }.thenReturn(Document("age", Document("\$gt", 18)))
        }
        whenever(compiler.compile(predicate)).thenReturn(CompilationResult(expressionToken))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(operation))

        // Act
        val aggregation = translator.translate(query)

        // Assert
        val stages = stagesOf(aggregation)
        assertThat(stages).hasSize(1)

        val stage = stages.first()
        assertThat(stage).containsKey("\$match")
        assertThat(stage["\$match"].toString())
            .contains("age")
            .contains("\$gt")
    }

    @Test
    fun `should translate ProjectionOperation with StatementToken into project and replaceRoot`() {
        // Arrange
        val projection = Expression.root<Any>()
        val operation = ProjectionOperation(projection)

        val statementToken = mock<StatementToken> {
            on { toStatement() }.thenReturn("some.path")
        }
        whenever(compiler.compile(projection)).thenReturn(CompilationResult(statementToken))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(operation))

        // Act
        val aggregation = translator.translate(query)

        // Assert
        val stages = stagesOf(aggregation)
        assertThat(stages).hasSize(2)

        val firstStage = stages[0]
        val secondStage = stages[1]

        assertThat(firstStage).containsKey("\$project")
        assertThat(secondStage).containsKey("\$replaceRoot")
    }

    @Test
    fun `should translate SortingOperation with StatementToken into sort stage`() {
        // Arrange
        val expression = Expression.root<Any>()
        val sort = Sort(expression, SortDirection.Descending)
        val sortingOperation = SortingOperation(Sorting(listOf(sort)))

        val statementToken = mock<StatementToken> {
            on { toStatement() }.thenReturn("user.age")
        }
        whenever(compiler.compile(expression)).thenReturn(CompilationResult(statementToken))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(sortingOperation))

        // Act
        val aggregation = translator.translate(query)

        // Assert
        val stages = stagesOf(aggregation)
        assertThat(stages).hasSize(1)

        val stage = stages.first()
        assertThat(stage).containsKey("\$sort")

        val sortSpec = stage["\$sort"] as Document
        assertThat(sortSpec.getInteger("user.age")).isEqualTo(-1)
    }

    @Test
    fun `sorting should append a unique _id tiebreaker to keep pagination stable`() {
        // Arrange
        val expression = Expression.root<Any>()
        val sort = Sort(expression, SortDirection.Descending)
        val sortingOperation = SortingOperation(Sorting(listOf(sort)))

        val statementToken = mock<StatementToken> {
            on { toStatement() }.thenReturn("model.metaInformationen.bearbeitet.actor.lastName")
        }
        whenever(compiler.compile(expression)).thenReturn(CompilationResult(statementToken))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(sortingOperation))

        // Act
        val aggregation = translator.translate(query)

        // Assert
        val sortSpec = stagesOf(aggregation).single()["\$sort"] as Document

        // A non-unique sort key (e.g. lastName) must be disambiguated by a unique field so that
        // $skip / $limit pagination produces a deterministic, gap-free ordering across pages.
        assertThat(sortSpec.keys).containsExactly(
            "model.metaInformationen.bearbeitet.actor.lastName",
            "_id"
        )
        assertThat(sortSpec.getInteger("_id")).isEqualTo(1)
    }

    @Test
    fun `should translate LimitOperation into limit stage`() {
        // Arrange
        val limitOp = LimitOperation(10)
        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(limitOp))

        // Act
        val aggregation = translator.translate(query)

        // Assert
        val stages = stagesOf(aggregation)
        assertThat(stages).hasSize(1)

        val stageDoc = stages.first()
        assertThat(stageDoc).containsKey("\$limit")
        assertThat(stageDoc["\$limit"]).isEqualTo(10L)
    }

    @Test
    fun `should throw QueryExecutionException for invalid filter token`() {
        // Arrange
        val predicate: PredicateExpression<Any> = Expression.root<Any>().isEqual(true)
        val operation = FilterOperation(predicate)

        whenever(compiler.compile(predicate)).thenReturn(CompilationResult(ConstantToken(42)))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(operation))

        // Act & Assert
        assertThatThrownBy { translator.translate(query) }
            .isInstanceOf(QueryExecutionException::class.java)
            .hasMessageContaining("Cannot filter by")
    }

    @Test
    fun `should throw QueryExecutionException for invalid projection token`() {
        // Arrange
        val projection = Expression.root<Any>()
        val operation = ProjectionOperation(projection)

        whenever(compiler.compile(projection)).thenReturn(CompilationResult(ConstantToken(42)))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(operation))

        // Act & Assert
        assertThatThrownBy { translator.translate(query) }
            .isInstanceOf(QueryExecutionException::class.java)
            .hasMessageContaining("Cannot project by")
    }

    @Test
    fun `should throw QueryExecutionException for invalid sorting token`() {
        // Arrange
        val expression = Expression.root<Any>()
        val sort = Sort(expression, SortDirection.Ascending)
        val operation = SortingOperation(Sorting(listOf(sort)))

        whenever(compiler.compile(expression)).thenReturn(CompilationResult(ConstantToken("oops")))

        val query = mock<FlowQuery<Any, Any>>()
        whenever(query.operations).thenReturn(listOf(operation))

        // Act & Assert
        assertThatThrownBy { translator.translate(query) }
            .isInstanceOf(QueryExecutionException::class.java)
            .hasMessageContaining("Cannot sort by")
    }
}