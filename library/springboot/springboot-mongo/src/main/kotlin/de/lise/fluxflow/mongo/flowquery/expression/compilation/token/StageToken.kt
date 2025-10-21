package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.springframework.data.mongodb.core.aggregation.AggregationOperation

internal interface StageToken : MongoToken {
    fun toStage(): AggregationOperation
}