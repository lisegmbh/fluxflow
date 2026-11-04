package de.lise.fluxflow.mongo

import org.springframework.data.mongodb.core.aggregation.AggregationOperation

/**
 * Supplies additional raw-document stages for a workflow FlowQuery execution.
 *
 * Ordered Spring beans are consulted once for each execution. Their stages precede all
 * query operations, including projections and limits, and the same stages are used for
 * both page contents and their count. The callback receives the persisted root document
 * type, even when the query projects to another result type.
 *
 * Providers must return read-only stages that preserve the root document shape. They
 * should return an empty list outside their explicitly scoped operation. FluxFlow does
 * not interpret permissions or establish request scope; the application owns that policy.
 * Returning no stages preserves the ordinary query path. Provider failures propagate.
 *
 * This extension applies to workflow FlowQuery queries; direct ID reads and other
 * repositories retain their existing behavior.
 */
fun interface FluxFlowMongoQueryPipelineProvider {
    fun stages(rootType: Class<*>): List<AggregationOperation>
}
