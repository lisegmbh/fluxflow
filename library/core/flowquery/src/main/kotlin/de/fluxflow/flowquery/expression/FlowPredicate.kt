package de.fluxflow.flowquery.expression

typealias FlowPredicate<TRoot> = Expression<TRoot, Boolean>

fun <TRoot> FlowPredicate<TRoot>.and(vararg others: FlowPredicate<TRoot>): AndOperator<TRoot> {
    return AndOperator(
        listOf(
            this,
            *others,
        )
    )
}

fun <TRoot> FlowPredicate<TRoot>.or(vararg others: FlowPredicate<TRoot>): OrOperator<TRoot> {
    return OrOperator(
        listOf(
            this,
            *others
        )
    )
}

fun <TRoot> FlowPredicate<TRoot>.not(): NotOperator<TRoot> {
    return NotOperator(
        this
    )
}