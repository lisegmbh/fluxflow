package de.fluxflow.flowquery.mapper.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionNode
import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.query.sorting.Sort
import de.fluxflow.flowquery.query.sorting.Sorting

@Suppress("UNCHECKED_CAST")
class QueryMapperImpl<TFromRoot, TToRoot>(
    private val expressionMapper: ExpressionMapper
): QueryMapper<TFromRoot, TToRoot> {

    override fun <TNewResult> map(query: FlowQuery<TFromRoot, *>): FlowQuery<TToRoot, TNewResult> {
        val expressionNode = ExpressionNode.root(query.cursor)
        return FlowQueryImpl(
            cursor = expressionMapper.map(expressionNode) as Expression<TToRoot, TNewResult>,
            operations = query.operations.map {
                mapOperation(it)
            },
            pagination = query.pagination
        )
    }

    private fun mapOperation(operation: QueryOperation): QueryOperation {
        return when(operation) {
            is FilterOperation -> FilterOperation(
                predicate = expressionMapper.map(
                    ExpressionNode.root(operation.predicate)
                        .withExpectedType<Boolean>()
                ) as Expression<*, Boolean>
            )
            is ProjectionOperation -> ProjectionOperation(
                projection = expressionMapper.map(
                    ExpressionNode.root(operation.projection)
                )
            )
            is SortingOperation -> SortingOperation(
                sorting = Sorting(
                    sorts = operation.sorting.sorts.map {
                        Sort(
                            expression = expressionMapper.map(
                                ExpressionNode.root(it.expression)
                            ),
                            direction = it.direction
                        )
                    }
                )
            )
            is LimitOperation -> operation // No need to map limit operations
        }
    }


}