package de.fluxflow.flowquery.mapper.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.query.sorting.Sort
import de.fluxflow.flowquery.query.sorting.Sorting

@Suppress("UNCHECKED_CAST")
class QueryMapperImpl<TFromRoot, TToRoot>(
    private val expressionMapper: ExpressionMapper
): QueryMapper<TFromRoot, TToRoot> {

    override fun <TNewResult> map(query: FlowQuery<TFromRoot, *>): FlowQuery<TToRoot, TNewResult> {
        return FlowQueryImpl(
            cursor = expressionMapper.map(query.cursor) as Expression<TToRoot, TNewResult>,
            operations = query.operations.map {
                mapOperation(it)
            },
            pagination = query.pagination
        )
    }

    private fun mapOperation(operation: QueryOperation): QueryOperation {
        return when(operation) {
            is FilterOperation -> FilterOperation(
                predicate = expressionMapper.map(operation.predicate) as Expression<*, Boolean>
            )
            is ProjectionOperation -> ProjectionOperation(
                projection = expressionMapper.map(operation.projection)
            )
            is SortingOperation -> SortingOperation(
                sorting = Sorting(
                    sorts = operation.sorting.sorts.map {
                        Sort(
                            expression = expressionMapper.map(it.expression),
                            direction = it.direction
                        )
                    }
                )
            )
            is LimitOperation -> operation // No need to map limit operations
        }
    }


}