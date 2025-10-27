package de.fluxflow.flowquery.query

class UnsupportedQueryOperationException(operation: QueryOperation) : QueryExecutionException(
    "Unsupported query operation ${operation::class.simpleName}: ${operation.toText()}."
)