package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.springframework.data.mongodb.core.aggregation.AggregationOperation

/**
 * Represents a token that can be converted to a MongoDB aggregation stage
 */
internal interface StageToken : MongoToken {
    fun toStage(): AggregationOperation
}