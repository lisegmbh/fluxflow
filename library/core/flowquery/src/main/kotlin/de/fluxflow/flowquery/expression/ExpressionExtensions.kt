package de.fluxflow.flowquery.expression

import kotlin.reflect.KClass

/**
 * Provides extension functions for building and combining [Expression]s in a fluent, DSL-like style.
 *
 * The extensions are grouped by domain (logical, string, collection, type, and map operations)
 * and create composable expression trees that can be transformed into queries or evaluated dynamically.
 */
object ExpressionExtensions {

    // --------------------------------------------------------------------------------------------
    // Logical Operations
    // --------------------------------------------------------------------------------------------

    /**
     * Provides logical combination and negation operations for predicate expressions.
     */
    object Logical {

        /**
         * Combines two predicate expressions using a logical AND.
         *
         * Example:
         * ```
         * val expr = user.get(User::isActive).isEqual(true) and user.get(User::age).isGreaterThan(18)
         * ```
         *
         * @param other the predicate to combine with
         * @return an [AndExpression] representing the conjunction of both predicates
         */
        infix fun <TRoot> PredicateExpression<TRoot>.and(other: PredicateExpression<TRoot>): AndExpression<TRoot> {
            return AndExpression(listOf(this, other))
        }

        /**
         * Combines this predicate with a list of additional predicates using a logical AND.
         *
         * @param others a list of additional predicate expressions
         * @return an [AndExpression] representing the conjunction of all predicates
         */
        fun <TRoot> PredicateExpression<TRoot>.and(others: List<PredicateExpression<TRoot>>): AndExpression<TRoot> {
            return AndExpression(listOf(this) + others)
        }

        /**
         * Combines this predicate with another predicate built from the root expression using a logical AND.
         *
         * Example:
         * ```
         * val expr = user.get(User::isActive).isEqual(true).and {
         *     get(User::age).isGreaterThan(18)
         * }
         * ```
         *
         * @param builder a builder function creating another predicate expression from the root
         * @return an [AndExpression] representing the conjunction of both predicates
         */
        fun <TRoot> PredicateExpression<TRoot>.and(
            builder: ExpressionBuilder<TRoot, TRoot, Boolean>,
        ): AndExpression<TRoot> {
            return AndExpression(listOf(this, builder(Expression.root())))
        }

        /**
         * Combines two predicate expressions using a logical OR.
         *
         * Example:
         * ```
         * val expr = user.get(User::role).isEqual("admin") or user.get(User::role).isEqual("manager")
         * ```
         *
         * @param other the predicate to combine with
         * @return an [OrExpression] representing the disjunction of both predicates
         */
        infix fun <TRoot> PredicateExpression<TRoot>.or(other: PredicateExpression<TRoot>): OrExpression<TRoot> {
            return OrExpression(listOf(this, other))
        }

        /**
         * Combines this predicate with a list of additional predicates using a logical OR.
         *
         * @param others a list of additional predicate expressions
         * @return an [OrExpression] representing the disjunction of all predicates
         */
        fun <TRoot> PredicateExpression<TRoot>.or(others: List<PredicateExpression<TRoot>>): OrExpression<TRoot> {
            return OrExpression(listOf(this) + others)
        }

        /**
         * Combines this predicate with another predicate built from the root expression using a logical OR.
         *
         * Example:
         * ```
         * val expr = user.get(User::role).isEqual("admin").or {
         *     get(User::role).isEqual("manager")
         * }
         * ```
         *
         * @param builder a builder function creating another predicate expression from the root
         * @return an [OrExpression] representing the disjunction of both predicates
         */
        fun <TRoot> PredicateExpression<TRoot>.or(
            builder: ExpressionBuilder<TRoot, TRoot, Boolean>,
        ): PredicateExpression<TRoot> {
            return OrExpression(listOf(this, builder(Expression.root())))
        }

        /**
         * Negates this predicate expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::isActive).isEqual(false).not()
         * ```
         *
         * @return a [NotExpression] representing the negation of this predicate
         */
        fun <TRoot> PredicateExpression<TRoot>.not(): NotExpression<TRoot> {
            return NotExpression(this)
        }
    }


    // --------------------------------------------------------------------------------------------
    // String Operations
    // --------------------------------------------------------------------------------------------

    /**
     * Provides string-related operations for expressions, such as `startsWith`, `endsWith`, and `contains`.
     */
    object Strings {

        /**
         * Creates a predicate that checks whether this string expression starts with the given prefix expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::email).startsWith(other.get(User::domain))
         * ```
         *
         * @param prefix the prefix expression to match
         * @param ignoreCasing whether the comparison should ignore case (default: true)
         * @return a [StartsWithExpression] representing the predicate
         */
        fun <TRoot> Expression<TRoot, String>.startsWith(
            prefix: Expression<TRoot, String>,
            ignoreCasing: Boolean = true,
        ): StartsWithExpression<TRoot> {
            return StartsWithExpression(value = this, prefix = prefix, ignoreCasing = ignoreCasing)
        }

        /**
         * Creates a predicate that checks whether this string expression starts with the given constant prefix.
         *
         * Example:
         * ```
         * val expr = user.get(User::email).startsWith("admin@")
         * ```
         *
         * @param prefix the constant prefix to match
         * @param ignoreCasing whether the comparison should ignore case (default: true)
         * @return a [StartsWithExpression] representing the predicate
         */
        fun <TRoot> Expression<TRoot, String>.startsWith(
            prefix: String,
            ignoreCasing: Boolean = true,
        ): StartsWithExpression<TRoot> {
            return this.startsWith(Expression.const(prefix), ignoreCasing)
        }

        /**
         * Creates a predicate that checks whether this string expression ends with the given suffix expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::email).endsWith(other.get(User::domain))
         * ```
         *
         * @param suffix the suffix expression to match
         * @param ignoreCasing whether the comparison should ignore case (default: true)
         * @return an [EndsWithExpression] representing the predicate
         */
        fun <TRoot> Expression<TRoot, String>.endsWith(
            suffix: Expression<TRoot, String>,
            ignoreCasing: Boolean = true,
        ): EndsWithExpression<TRoot> {
            return EndsWithExpression(value = this, suffix = suffix, ignoreCasing = ignoreCasing)
        }

        /**
         * Creates a predicate that checks whether this string expression ends with the given constant suffix.
         *
         * Example:
         * ```
         * val expr = user.get(User::email).endsWith(".com")
         * ```
         *
         * @param suffix the constant suffix to match
         * @param ignoreCasing whether the comparison should ignore case (default: true)
         * @return an [EndsWithExpression] representing the predicate
         */
        fun <TRoot> Expression<TRoot, String>.endsWith(
            suffix: String,
            ignoreCasing: Boolean = true,
        ): EndsWithExpression<TRoot> {
            return this.endsWith(Expression.const(suffix), ignoreCasing)
        }

        /**
         * Creates a predicate that checks whether this string expression contains the given substring expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::name).contains(other.get(User::nickname))
         * ```
         *
         * @param substring the substring expression to search for
         * @param ignoreCasing whether the comparison should ignore case (default: true)
         * @return a [ContainsExpression] representing the predicate
         */
        fun <TRoot> Expression<TRoot, String>.contains(
            substring: Expression<TRoot, String>,
            ignoreCasing: Boolean = true,
        ): ContainsExpression<TRoot> {
            return ContainsExpression(value = this, substring = substring, ignoreCasing = ignoreCasing)
        }

        /**
         * Creates a predicate that checks whether this string expression contains the given constant substring.
         *
         * Example:
         * ```
         * val expr = user.get(User::name).contains("John")
         * ```
         *
         * @param substring the constant substring to search for
         * @param ignoreCasing whether the comparison should ignore case (default: true)
         * @return a [ContainsExpression] representing the predicate
         */
        fun <TRoot> Expression<TRoot, String>.contains(
            substring: String,
            ignoreCasing: Boolean = true,
        ): ContainsExpression<TRoot> {
            return this.contains(Expression.const(substring), ignoreCasing)
        }
    }


    // --------------------------------------------------------------------------------------------
    // Collection Operations
    // --------------------------------------------------------------------------------------------

    /**
     * Provides operations for working with collection-valued expressions.
     */
    object Collections {

        /**
         * Creates a predicate that checks whether the collection contains any element
         * satisfying the given predicate expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::tags).containsElementThat(tag.get(Tag::isActive))
         * ```
         *
         * @param elementPredicate the predicate to test each element
         * @return a [PredicateExpression] representing the containment check
         */
        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.containsElementThat(
            elementPredicate: PredicateExpression<TElement>,
        ): PredicateExpression<TRoot> {
            return ContainsElementThatExpression(this, elementPredicate)
        }

        /**
         * Creates a predicate that checks whether the collection contains any element
         * matching a predicate built from a new [RootExpression].
         *
         * Example:
         * ```
         * val expr = user.get(User::tags).containsElementThat {
         *     get(Tag::name).isEqual("featured")
         * }
         * ```
         *
         * @param builder a builder function for creating the element predicate
         * @return a [PredicateExpression] representing the containment check
         */
        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.containsElementThat(
            builder: RootExpression<TElement>.() -> PredicateExpression<TElement>,
        ): PredicateExpression<TRoot> {
            return this.containsElementThat(builder(Expression.root()))
        }

        /**
         * Creates a predicate that checks whether the collection contains the given element expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::roles).contains(other.get(User::role))
         * ```
         *
         * @param element the element expression to look for
         * @return a [ContainsElementExpression] representing the containment check
         */
        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.contains(
            element: Expression<TRoot, TElement>,
        ): ContainsElementExpression<TRoot, TCollection, TElement> {
            return ContainsElementExpression(this, element)
        }

        /**
         * Creates a predicate that checks whether the collection contains the given constant element.
         *
         * Example:
         * ```
         * val expr = user.get(User::roles).contains("ADMIN")
         * ```
         *
         * @param element the constant element to look for
         * @return a [ContainsElementExpression] representing the containment check
         */
        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.contains(
            element: TElement,
        ): ContainsElementExpression<TRoot, TCollection, TElement> {
            return contains(Expression.const(element))
        }
    }


    // --------------------------------------------------------------------------------------------
    // Type Operations
    // --------------------------------------------------------------------------------------------

    /**
     * Provides operations for type checking and casting within expression trees.
     */
    object Types {

        /**
         * Creates a predicate that checks whether this expression is of the specified type.
         *
         * Example:
         * ```
         * val expr = user.get(User::address).isType(ShippingAddress::class)
         * ```
         *
         * @param type the required type to check against
         * @return a [PredicateExpression] representing the type check
         */
        fun <TRoot, TCurrent : Any, TRequiredType : TCurrent> Expression<TRoot, TCurrent>.isType(
            type: KClass<TRequiredType>,
        ): PredicateExpression<TRoot> {
            return IsTypeExpression(this, type)
        }

        /**
         * Creates a cast expression converting this expression to a more specific type.
         *
         * Example:
         * ```
         * val expr = user.get(User::address).asType(ShippingAddress::class)
         * ```
         *
         * @param type the target type to cast to
         * @return a [CastExpression] representing the cast
         */
        fun <TRoot, TCurrent : Any, TRequiredType : TCurrent> Expression<TRoot, in TCurrent>.asType(
            type: KClass<TRequiredType>,
        ): CastExpression<TRoot, TCurrent, TRequiredType> {
            return CastExpression(this as Expression<TRoot, TCurrent>, type)
        }
    }


    // --------------------------------------------------------------------------------------------
    // Map Operations
    // --------------------------------------------------------------------------------------------

    /**
     * Provides operations for accessing map values within expression trees.
     */
    object Maps {

        /**
         * Creates an expression that accesses a value in a map by the given key expression.
         *
         * Example:
         * ```
         * val expr = user.get(User::settings).get(other.get(Setting::key))
         * ```
         *
         * @param key the key expression
         * @return an [Expression] representing the map value at the specified key
         */
        fun <TRoot, TCurrent : Map<TKey, TValue>, TKey, TValue> Expression<TRoot, TCurrent>.get(
            key: Expression<TRoot, TKey>,
        ): Expression<TRoot, TValue> {
            return MapAccessExpression(this, key)
        }
    }
}