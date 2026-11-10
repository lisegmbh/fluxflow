package de.lise.fluxflow.mongo

import de.fluxflow.flowquery.query.QueryConstraint
import org.springframework.data.mongodb.core.aggregation.AggregationOperation

/**
 * Translates an explicit per-execution constraint into read-only raw-document stages.
 * Return null for unsupported constraints; an empty list explicitly accepts an unrestricted constraint.
 * At least one ordered provider must accept the constraint, otherwise execution fails before querying.
 * Stages preserve the root shape and precede projection, limits, count and pagination.
 * Ordinary calls without a constraint do not consult providers. No thread-local context is used.
 */
fun interface FluxFlowMongoQueryPipelineProvider {
    fun stages(rootType: Class<*>, constraint: QueryConstraint): List<AggregationOperation>?
}
