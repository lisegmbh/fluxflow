package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.Fields

internal class ProjectToken(
    val fieldName: String,
    val statementToken: StatementToken
): StageToken {
    override fun toStage(): AggregationOperation {
        return Aggregation.project(
            Fields.from(
                Fields.field(
                    fieldName,
                    statementToken.toStatement()
                )
            )
        )
    }
}