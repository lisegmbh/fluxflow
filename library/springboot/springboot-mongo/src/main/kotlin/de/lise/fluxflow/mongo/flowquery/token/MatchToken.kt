package de.lise.fluxflow.mongo.flowquery.token

import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation

internal class MatchToken(
    val expression: ExpressionToken
) : StageToken {
    override fun toStage(): AggregationOperation {
        return Aggregation.match { _ -> expression.toExpression() }
    }
}

