package de.fluxflow.flowquery.expression

typealias ExpressionBuilder<TRoot, TCurrent, TTarget> = Expression<TRoot, TCurrent>.() -> Expression<TRoot, TTarget>