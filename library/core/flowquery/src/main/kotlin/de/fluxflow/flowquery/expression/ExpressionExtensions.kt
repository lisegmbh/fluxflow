package de.fluxflow.flowquery.expression

import kotlin.reflect.KClass

object ExpressionExtensions {
    object Logical {
        fun <TRoot> FlowPredicate<TRoot>.and(vararg others: FlowPredicate<TRoot>): AndExpression<TRoot> {
            return AndExpression(
                listOf(
                    this,
                    *others,
                )
            )
        }
        
        fun <TRoot> FlowPredicate<TRoot>.and(
            builder: ExpressionBuilder<TRoot, TRoot, Boolean>,
        ): AndExpression<TRoot> {
            return AndExpression(
                listOf(
                    this, 
                    builder(Expression.root())
                )
            )
        }

        fun <TRoot> FlowPredicate<TRoot>.or(vararg others: FlowPredicate<TRoot>): OrExpression<TRoot> {
            return OrExpression(
                listOf(
                    this,
                    *others
                )
            )
        }

        fun <TRoot> FlowPredicate<TRoot>.or(
            builder: ExpressionBuilder<TRoot, TRoot, Boolean>
        ): FlowPredicate<TRoot> {
            return OrExpression(
                listOf(
                    this,
                    builder(Expression.root())
                )
            )
        }
        
        fun <TRoot> FlowPredicate<TRoot>.not(): NotOperator<TRoot> {
            return NotOperator(
                this
            )
        }
    }

    object Strings {
        fun <TRoot> Expression<TRoot, String>.startsWith(
            prefix: Expression<TRoot, String>,
            ignoreCasing: Boolean = true
        ): StartsWithExpression<TRoot> {
            return StartsWithExpression(
                value = this,
                prefix = prefix,
                ignoreCasing = ignoreCasing
            )
        }

        fun <TRoot> Expression<TRoot, String>.startsWith(
            prefix: String,
            ignoreCasing: Boolean = true
        ): StartsWithExpression<TRoot> {
            return this.startsWith(
                Expression.const(prefix),
                ignoreCasing
            )
        }

        fun <TRoot> Expression<TRoot, String>.endsWith(
            suffix: Expression<TRoot, String>,
            ignoreCasing: Boolean = true
        ): EndsWithExpression<TRoot> {
            return EndsWithExpression(
                value = this,
                suffix = suffix,
                ignoreCasing = ignoreCasing
            )
        }

        fun <TRoot> Expression<TRoot, String>.endsWith(
            suffix: String,
            ignoreCasing: Boolean = true
        ): EndsWithExpression<TRoot> {
            return this.endsWith(
                Expression.const(suffix),
                ignoreCasing
            )
        }

        fun <TRoot> Expression<TRoot, String>.contains(
            substring: Expression<TRoot, String>,
            ignoreCasing: Boolean = true
        ): ContainsExpression<TRoot> {
            return ContainsExpression(
                value = this,
                substring = substring,
                ignoreCasing = ignoreCasing
            )
        }

        fun <TRoot> Expression<TRoot, String>.contains(
            substring: String,
            ignoreCasing: Boolean = true
        ): ContainsExpression<TRoot> {
            return this.contains(
                substring = Expression.const(substring),
                ignoreCasing = ignoreCasing
            )
        }
    }

    object Collections {
        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.containsElementThat(
            elementPredicate: FlowPredicate<TElement>
        ): FlowPredicate<TRoot> {
            return ContainsElementThatExpression(
                this,
                elementPredicate
            )
        }

        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.containsElementThat(
            builder: Root<TElement>.() -> FlowPredicate<TElement>
        ): FlowPredicate<TRoot> {
            return this.containsElementThat(
                builder(Expression.root())
            )
        }

        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.contains(
            element: Expression<TRoot, TElement>
        ): ContainsElementExpression<TRoot, TCollection, TElement> {
            return ContainsElementExpression(
                this,
                element
            )
        }

        fun <TRoot, TCollection : Collection<TElement>, TElement> Expression<TRoot, TCollection>.contains(
            element: TElement
        ): ContainsElementExpression<TRoot, TCollection, TElement> {
            return contains(
                Expression.const(element)
            )
        }
    }

    object Types {
        fun <TRoot, TCurrent: Any, TRequiredType: TCurrent> Expression<TRoot, TCurrent>.isType(
            type: KClass<TRequiredType>
        ): FlowPredicate<TRoot> {
            return IsTypeExpression(
                this,
                type
            )
        }

        fun <TRoot, TCurrent: Any, TRequiredType: TCurrent> Expression<TRoot, in TCurrent>.asType(
            type: KClass<TRequiredType>
        ): CastExpression<TRoot, TCurrent, TRequiredType> {
            return CastExpression(
                this as Expression<TRoot, TCurrent>,
                type
            )
        }
    }

    object Maps {
        fun <TRoot, TCurrent : Map<TKey, TValue>, TKey, TValue> Expression<TRoot, TCurrent>.get(
            key: Expression<TRoot, TKey>
        ): Expression<TRoot, TValue> {
            return MapAccessExpression(
                this,
                key
            )
        }
    }
}

