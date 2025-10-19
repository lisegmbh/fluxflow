package de.fluxflow.flowquery.expression

object ExpressionExtensions {

    object Logical {
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
    }
}