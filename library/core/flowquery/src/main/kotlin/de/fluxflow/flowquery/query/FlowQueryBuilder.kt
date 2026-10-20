package de.fluxflow.flowquery.query

typealias FlowQueryBuilder<TRoot, TResult> = FlowQuery<TRoot, TRoot>.() -> FlowQuery<TRoot, TResult>