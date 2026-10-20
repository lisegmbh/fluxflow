package de.fluxflow.flowquery.query

data class LimitOperation(
    val amount: Long
): QueryOperation {
    override fun toText(): String {
        return "LIMIT $amount"
    }

    override fun toString(): String {
        return toText()
    }
}