package de.fluxflow.flowquery.expression

import kotlin.reflect.KProperty1

/**
 * Represents a node within a type-safe query expression tree.
 *
 * Expressions model structured query logic similar to SQL or LINQ,
 * allowing composable building of predicates and projections over a given root type [TRoot].
 *
 * Each [Expression] instance corresponds to a node in an abstract syntax tree (AST)
 * that can later be translated into a concrete query language or evaluated dynamically.
 *
 * @param TRoot the root entity or object type of the expression tree
 * @param TCurrent the value type represented by this expression node
 */
sealed interface Expression<TRoot, TCurrent> {

    /**
     * Returns a textual representation of this expression, such as `"user.age > 18"`.
     *
     * Primarily intended for debugging, logging, or visualization purposes.
     *
     * @return a human-readable textual representation of this expression
     */
    fun toText(): String


    // --------------------------------------------------------------------------------------------
    // Logical Composition
    // --------------------------------------------------------------------------------------------

    /**
     * Builds a logical AND expression that combines all given boolean subexpressions.
     *
     * Each [ExpressionBuilder] is evaluated with this expression as its receiver,
     * allowing concise composition within DSLs.
     *
     * Example:
     * ```
     * user.allTrue(
     *   { get(User::isActive).isEqual(true) },
     *   { get(User::age).isGreaterThan(18) }
     * )
     * ```
     *
     * @param builders one or more lambda builders producing boolean expressions
     * @return a combined [AndExpression] that evaluates to true only if all subexpressions are true
     */
    fun allTrue(
        vararg builders: ExpressionBuilder<TRoot, TCurrent, Boolean>
    ): AndExpression<TRoot> {
        return builders.map { it(this) }.let { AndExpression(it) }
    }

    /**
     * Builds a logical OR expression that combines all given boolean subexpressions.
     *
     * Example:
     * ```
     * user.anyTrue(
     *   { get(User::role).isEqual("admin") },
     *   { get(User::role).isEqual("manager") }
     * )
     * ```
     *
     * @param builders one or more lambda builders producing boolean expressions
     * @return a combined [OrExpression] that evaluates to true if any subexpression is true
     */
    fun anyTrue(
        vararg builders: ExpressionBuilder<TRoot, TCurrent, Boolean>
    ): OrExpression<TRoot> {
        return builders.map { it(this) }.let { OrExpression(it) }
    }


    // --------------------------------------------------------------------------------------------
    // Property Access
    // --------------------------------------------------------------------------------------------

    /**
     * Creates a new expression representing access to a property of the current type.
     *
     * Example:
     * ```
     * val ageExpr = user.get(User::age)
     * ```
     *
     * @param prop the property to access on the current expression's value
     * @return a [PropertyExpression] representing the accessed property
     */
    fun <TProperty> get(
        prop: KProperty1<out TCurrent, TProperty?>
    ): PropertyExpression<TRoot, TCurrent, TProperty> {
        return PropertyExpression(this, prop as KProperty1<TCurrent, TProperty>)
    }


    // --------------------------------------------------------------------------------------------
    // Predicate Operations (Comparisons)
    // --------------------------------------------------------------------------------------------

    /**
     * Creates an equality comparison between this expression and another.
     *
     * Example:
     * ```
     * val user = Expression.root<User>()
     * val expr = user.get(User::age).isEqual(other.get(User::age))
     * ```
     *
     * @param exp the expression to compare with
     * @return a [PredicateExpression] representing the equality comparison
     */
    fun <TOther> isEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(this, BinaryOperation.Equal, exp)
    }

    /**
     * Creates an equality comparison between this expression and a constant value.
     *
     * Example:
     * ```
     * val expr = user.get(User::age).isEqual(30)
     * ```
     *
     * @param value the constant value to compare with
     * @return a [PredicateExpression] representing the equality comparison
     */
    fun isEqual(value: TCurrent?): PredicateExpression<TRoot> {
        return isEqual(const(value))
    }

    /**
     * Creates an inequality comparison between this expression and another.
     *
     * Example:
     * ```
     * val expr = user.get(User::role).isNotEqual(other.get(User::role))
     * ```
     *
     * @param exp the expression to compare with
     * @return a [PredicateExpression] representing the inequality comparison
     */
    fun <TOther> isNotEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(this, BinaryOperation.NotEqual, exp)
    }

    /**
     * Creates an inequality comparison between this expression and a constant value.
     *
     * Example:
     * ```
     * val expr = user.get(User::role).isNotEqual("guest")
     * ```
     *
     * @param value the constant value to compare with
     * @return a [PredicateExpression] representing the inequality comparison
     */
    fun isNotEqual(value: TCurrent): PredicateExpression<TRoot> {
        return isNotEqual(const(value))
    }

    /**
     * Creates a predicate that tests whether this expression's value is contained in a given collection.
     *
     * Example:
     * ```
     * val expr = user.get(User::status).isAnyOf(listOf("ACTIVE", "PENDING"))
     * ```
     *
     * @param others the collection of possible values
     * @return an [IsAnyOfOperator] that checks whether this expression matches any of the given values
     */
    fun isAnyOf(others: Collection<TCurrent>): IsAnyOfOperator<TRoot, TCurrent> {
        return IsAnyOfOperator(this, others.map { ConstantExpression<TRoot, TCurrent>(it) }.toSet())
    }

    /**
     * Creates a predicate that tests whether this expression's value is contained in one of the given values.
     *
     * Example:
     * ```
     * val expr = user.get(User::status).isAnyOf("ACTIVE", "PENDING")
     * ```
     *
     * @param others one or more constant values to compare with
     * @return an [IsAnyOfOperator] that checks whether this expression matches any of the given values
     */
    fun isAnyOf(vararg others: TCurrent): IsAnyOfOperator<TRoot, TCurrent> {
        return isAnyOf(others.toSet())
    }

    /**
     * Creates a "less than" comparison between this expression and another.
     *
     * Example:
     * ```
     * val expr = user.get(User::age).isLessThan(other.get(User::minAge))
     * ```
     *
     * @param exp the expression to compare with
     * @return a [PredicateExpression] representing the "less than" comparison
     */
    fun <TOther : Comparable<TCurrent>> isLessThan(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(this, BinaryOperation.LessThan, exp)
    }

    /**
     * Creates a "less than" comparison between this expression and a constant value.
     *
     * Example:
     * ```
     * val expr = user.get(User::age).isLessThan(18)
     * ```
     *
     * @param other the constant value to compare with
     * @return a [PredicateExpression] representing the "less than" comparison
     */
    fun <TOther : Comparable<TCurrent>> isLessThan(other: TOther): PredicateExpression<TRoot> {
        return isLessThan(const(other))
    }

    /**
     * Creates a "less than or equal" comparison between this expression and another.
     *
     * @param exp the expression to compare with
     * @return a [PredicateExpression] representing the "less than or equal" comparison
     */
    fun <TOther : Comparable<TCurrent>> isLessThanOrEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(this, BinaryOperation.LessThanOrEqual, exp)
    }

    /**
     * Creates a "less than or equal" comparison between this expression and a constant value.
     *
     * @param other the constant value to compare with
     * @return a [PredicateExpression] representing the "less than or equal" comparison
     */
    fun <TOther : Comparable<TCurrent>> isLessThanOrEqual(other: TOther): PredicateExpression<TRoot> {
        return isLessThanOrEqual(const(other))
    }

    /**
     * Creates a "greater than" comparison between this expression and another.
     *
     * Example:
     * ```
     * val expr = user.get(User::age).isGreaterThan(other.get(User::minAge))
     * ```
     *
     * @param exp the expression to compare with
     * @return a [PredicateExpression] representing the "greater than" comparison
     */
    fun <TOther : Comparable<TCurrent>> isGreaterThan(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(this, BinaryOperation.GreaterThan, exp)
    }

    /**
     * Creates a "greater than" comparison between this expression and a constant value.
     *
     * Example:
     * ```
     * val expr = user.get(User::age).isGreaterThan(18)
     * ```
     *
     * @param other the constant value to compare with
     * @return a [PredicateExpression] representing the "greater than" comparison
     */
    fun <TOther : Comparable<TCurrent>> isGreaterThan(other: TOther): PredicateExpression<TRoot> {
        return isGreaterThan(const(other))
    }

    /**
     * Creates a "greater than or equal" comparison between this expression and another.
     *
     * @param exp the expression to compare with
     * @return a [PredicateExpression] representing the "greater than or equal" comparison
     */
    fun <TOther : Comparable<TCurrent>> isGreaterThanOrEqual(exp: Expression<TRoot, TOther>): PredicateExpression<TRoot> {
        return BinaryOperationExpression(this, BinaryOperation.GreaterThanOrEqual, exp)
    }

    /**
     * Creates a "greater than or equal" comparison between this expression and a constant value.
     *
     * @param other the constant value to compare with
     * @return a [PredicateExpression] representing the "greater than or equal" comparison
     */
    fun <TOther : Comparable<TCurrent>> isGreaterThanOrEqual(other: TOther): PredicateExpression<TRoot> {
        return isGreaterThanOrEqual(const(other))
    }


    // --------------------------------------------------------------------------------------------
    // Factory Functions
    // --------------------------------------------------------------------------------------------

    companion object {

        /**
         * Negates the given predicate expression.
         *
         * Example:
         * ```
         * val expr = Expression.not(user.get(User::isActive).isEqual(false))
         * ```
         *
         * @param predicate the predicate to negate
         * @return a [NotExpression] representing the logical negation of the given predicate
         */
        fun <TRoot> not(predicate: PredicateExpression<TRoot>): PredicateExpression<TRoot> {
            return NotExpression(predicate)
        }

        /**
         * Creates a constant expression holding a fixed value.
         *
         * Example:
         * ```
         * val constExpr = Expression.const<User, String>("admin")
         * ```
         *
         * @param value the constant value to wrap
         * @return a [ConstantExpression] representing the given value
         */
        fun <TRoot, T> const(value: T): ConstantExpression<TRoot, T> {
            return ConstantExpression(value)
        }

        /**
         * Creates a root expression that serves as the starting point of an expression tree.
         *
         * Typically used to begin building a query:
         * ```
         * val user = Expression.root<User>()
         * ```
         *
         * @return a [RootExpression] representing the root of a new expression tree
         */
        fun <T> root(): RootExpression<T> {
            return RootExpression()
        }
    }
}
