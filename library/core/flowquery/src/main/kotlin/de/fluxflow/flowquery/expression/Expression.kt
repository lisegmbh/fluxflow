package de.fluxflow.flowquery.expression

import kotlin.reflect.KProperty1

sealed interface Expression<TRoot, TCurrent> {
    fun asText(): String

    // Projections
    fun <TProperty : Any?> get(
        prop: KProperty1<TCurrent, TProperty?>
    ): PropertyExpression<TRoot, TCurrent, TProperty> {
        return PropertyExpression(
            this,
            prop
        )
    }

    // Logical operators
    fun and(vararg builders: Expression<TRoot, TCurrent>.() -> FlowPredicate<TRoot>): AndOperator<TRoot> {
        return builders.map { builder ->
            builder(this)
        }.let {
            AndOperator(it)
        }
    }

    fun or(vararg builders: Expression<TRoot, TCurrent>.() -> FlowPredicate<TRoot>): OrOperator<TRoot> {
        return builders.map { builder ->
            builder(this)
        }.let {
            OrOperator(it)
        }
    }

    fun not(builder: Expression<TRoot, TCurrent>.() -> FlowPredicate<TRoot>): NotOperator<TRoot> {
        return NotOperator(builder(this))
    }

    // Predicates
    fun <TOther> isEqual(exp: Expression<TRoot, TOther>): IsEqual<TRoot, TCurrent, TOther> {
        return IsEqual(this, exp)
    }

    fun isEqual(value: TCurrent): IsEqual<TRoot, TCurrent, TCurrent> {
        return IsEqual(
            this,
            const(value)
        )
    }

    fun isAnyOf(others: Collection<TCurrent>): IsAnyOfOperator<TRoot, TCurrent> {
        return IsAnyOfOperator(
            this,
            others.toSet()
        )
    }

    fun isAnyOf(vararg others: TCurrent): IsAnyOfOperator<TRoot, TCurrent> {
        return isAnyOf(others.toSet())
    }

    companion object {
        fun <TRoot> not(
            predicate: FlowPredicate<TRoot>
        ): FlowPredicate<TRoot> {
            return NotOperator(predicate)
        }

        fun <TRoot, T> const(value: T): Constant<TRoot, T> {
            return Constant(value)
        }

        fun <T> root(): Root<T> {
            return Root()
        }
    }
}

