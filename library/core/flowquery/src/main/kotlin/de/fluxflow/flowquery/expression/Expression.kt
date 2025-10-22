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
        prop: KProperty1<out TCurrent, TProperty?>
    ): PropertyExpression<TRoot, TCurrent, TProperty> {
        return PropertyExpression(
            this,
            prop as KProperty1<TCurrent,TProperty>
        )
    }

    // Predicates
    fun <TOther> isEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.Equal,
            exp
        )
    }

    fun isEqual(value: TCurrent?): PredicateExpression<TRoot> {
        return isEqual(
            const(value)
        )
    }


    fun <TOther> isNotEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.NotEqual,
            exp
        )
    }
    fun isNotEqual(value: TCurrent): PredicateExpression<TRoot> {
        return isNotEqual(const(value))
    }


    fun isAnyOf(others: Collection<TCurrent>): IsAnyOfOperator<TRoot, TCurrent> {
        return IsAnyOfOperator(
            this,
            others.map { ConstantExpression<TRoot,TCurrent>(it) }.toSet()
        )
    }

    fun isAnyOf(vararg others: TCurrent): IsAnyOfOperator<TRoot, TCurrent> {
        return isAnyOf(others.toSet())
    }

    fun <TOther : Comparable<TCurrent>> isLessThan(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.LessThan,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isLessThan(other: TOther): PredicateExpression<TRoot> {
        return isLessThan(
            const(other)
        )
    }

    fun <TOther : Comparable<TCurrent>> isLessThanOrEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.LessThanOrEqual,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isLessThanOrEqual(other: TOther): PredicateExpression<TRoot> {
        return isLessThanOrEqual(
            const(other)
        )
    }

    fun <TOther : Comparable<TCurrent>> isGreaterThan(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.GreaterThan,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isGreaterThan(other: TOther): PredicateExpression<TRoot> {
        return isGreaterThan(
            const(other)
        )
    }

    fun <TOther : Comparable<TCurrent>> isGreaterThanOrEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(
            this,
            BinaryOperation.GreaterThanOrEqual,
            exp
        )
    }
    fun <TOther: Comparable<TCurrent>> isGreaterThanOrEqual(other: TOther): PredicateExpression<TRoot> {
        return isGreaterThanOrEqual(
            const(other)
        )
    }

    companion object {
        fun <TRoot> not(
            predicate: PredicateExpression<TRoot>
        ): PredicateExpression<TRoot> {
            return NotExpression(predicate)
        }

        fun <TRoot, T> const(value: T): ConstantExpression<TRoot, T> {
            return ConstantExpression(value)
        }

        fun <T> root(): RootExpression<T> {
            return RootExpression()
        }
    }
}

