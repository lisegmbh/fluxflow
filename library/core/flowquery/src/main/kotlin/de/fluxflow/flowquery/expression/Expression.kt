package de.fluxflow.flowquery.expression

import kotlin.reflect.KProperty1

sealed interface Expression<TRoot, TCurrent> {
    fun toText(): String
    
    // Logical
    fun allTrue(
        vararg builders: ExpressionBuilder<TRoot, TCurrent, Boolean>
    ): AndExpression<TRoot> {
        return builders.map { 
            it(this)
        }.let {
            AndExpression(it)
        }
    }
    
    fun anyTrue(
        vararg builders: ExpressionBuilder<TRoot, TCurrent, Boolean>
    ): OrExpression<TRoot> {
        return builders.map { 
            it(this)
        }.let {
            OrExpression(it)
        }
    }
    
    // Projections
    fun <TProperty> get(
        prop: KProperty1<TCurrent, TProperty?>
    ): PropertyExpression<TRoot, TCurrent, TProperty> {
        return PropertyExpression(
            this,
            prop
        )
    }

    // Predicates
    fun <TOther> isEqual(exp: Expression<TRoot, TOther>): FlowPredicate<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.Equal,
            exp
        )
    }

    fun isEqual(value: TCurrent?): FlowPredicate<TRoot> {
        return isEqual(
            const(value)
        )
    }


    fun <TOther> isNotEqual(exp: Expression<TRoot, TOther>): FlowPredicate<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.NotEqual,
            exp
        )
    }
    fun isNotEqual(value: TCurrent): FlowPredicate<TRoot> {
        return isNotEqual(const(value))
    }


    fun isAnyOf(others: Collection<TCurrent>): IsAnyOfOperator<TRoot, TCurrent> {
        return IsAnyOfOperator(
            this,
            others.map { Constant<TRoot,TCurrent>(it) }.toSet()
        )
    }

    fun isAnyOf(vararg others: TCurrent): IsAnyOfOperator<TRoot, TCurrent> {
        return isAnyOf(others.toSet())
    }

    fun <TOther : Comparable<TCurrent>> isLessThan(exp: Expression<TRoot, TOther>): FlowPredicate<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.LessThan,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isLessThan(other: TOther): FlowPredicate<TRoot> {
        return isLessThan(
            const(other)
        )
    }

    fun <TOther : Comparable<TCurrent>> isLessThanOrEqual(exp: Expression<TRoot, TOther>): FlowPredicate<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.LessThanOrEqual,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isLessThanOrEqual(other: TOther): FlowPredicate<TRoot> {
        return isLessThanOrEqual(
            const(other)
        )
    }

    fun <TOther : Comparable<TCurrent>> isGreaterThan(exp: Expression<TRoot, TOther>): FlowPredicate<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.GreaterThan,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isGreaterThan(other: TOther): FlowPredicate<TRoot> {
        return isGreaterThan(
            const(other)
        )
    }

    fun <TOther : Comparable<TCurrent>> isGreaterThanOrEqual(exp: Expression<TRoot, TOther>): FlowPredicate<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.GreaterThanOrEqual,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isGreaterThanOrEqual(other: TOther): FlowPredicate<TRoot> {
        return isGreaterThanOrEqual(
            const(other)
        )
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

