package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.ConstantExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer.Companion.constantOfType
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer.Companion.property
import kotlin.reflect.KProperty1

/**
 * A *partial* transformation of a single [ExpressionNode].
 *
 * A replacer inspects a node and, if it applies, returns a replacement [Expression];
 * otherwise it returns `null` to signal that it does not handle the node. This makes
 * replacers composable: several can be tried in turn (see [PriorityExpressionReplacer])
 * until one produces a replacement.
 *
 * Replacers are commonly used to rewrite individual expression kinds — for example,
 * translating a domain identifier into its persisted scalar value — and can be promoted
 * to a tree-wide transformation via [recursive] or to a total [ExpressionMapper] via
 * [toMapper].
 *
 * As a Kotlin `fun interface`, a replacer can be created directly from a lambda:
 * ```
 * val replacer = ExpressionReplacer { node -> /* return replacement or null */ }
 * ```
 */
fun interface ExpressionReplacer {
    /**
     * Attempts to rewrite the given [node].
     *
     * @param node the expression node to inspect
     * @return the replacement expression, or `null` if this replacer does not apply to [node]
     */
    fun replace(node: ExpressionNode): Expression<*, *>?

    /**
     * Wraps this replacer so that it is applied recursively across an entire expression tree.
     *
     * The returned replacer walks the node's subtree, applying this replacer to each
     * expression and re-applying it to its own output until no further replacement occurs.
     *
     * @return a replacer that applies this replacer recursively over a whole expression tree
     */
    fun recursive(): ExpressionReplacer {
        return PriorityExpressionReplacer(
            listOf(
                this
            )
        )
    }

    /**
     * Adapts this replacer into a total [ExpressionMapper].
     *
     * The resulting mapper returns this replacer's replacement when it applies, and the
     * node's original [ExpressionNode.expression] otherwise.
     *
     * @return an [ExpressionMapper] backed by this replacer
     */
    fun toMapper(): ExpressionMapper {
        return ExpressionMapper {
            replace(it) ?: it.expression
        }
    }

    companion object {
        /**
         * Creates a replacer that rewrites [ConstantExpression]s whose value is of type [T].
         *
         * Only constant expressions are considered; for a matching constant, [replacement]
         * is invoked with the typed value to produce the new expression. All other
         * expressions (and constants of a different type) are left untouched.
         *
         * @param T the constant value type to match
         * @param replacement produces the replacement expression for a matched constant value
         * @return a replacer targeting constants of type [T]
         */
        inline fun <reified T> constantOfType(
            crossinline replacement: (exp: T) -> Expression<*,*>
        ): ExpressionReplacer {
            return ExpressionReplacer {
                when(val exp = it.expression) {
                    is ConstantExpression<*,*> if exp.value is T -> replacement(exp.value)
                    else -> null
                }
            }
        }
        
        /**
         * Creates a replacer that rewrites accesses to a specific property.
         *
         * Only [PropertyExpression]s referencing [prop] are considered; for a match,
         * [replacement] is invoked with the property expression and may itself return `null`
         * to decline the rewrite. All other expressions are left untouched.
         *
         * @param prop the property whose accesses should be rewritten
         * @param replacement produces the replacement expression for a matched property access,
         * or `null` to decline
         * @return a replacer targeting accesses to [prop]
         */
        fun property(
            prop: KProperty1<*,*>,
            replacement: (exp: PropertyExpression<*,*,*>) -> Expression<*,*>?
        ): ExpressionReplacer {
            return ExpressionReplacer {
                when(val exp = it.expression) {
                    is PropertyExpression<*,*,*> -> when(exp.property) {
                        prop -> replacement(exp)
                        else -> null
                    }
                    else -> null
                }
            }
        }

        /**
         * Creates a replacer that re-targets a property access from a source type to an
         * equivalent property on a target type.
         *
         * Accesses to [sourceProperty] are rewritten into a [PropertyExpression] reading
         * [targetProperty] from the same instance. This is typically used to translate a
         * property of a domain type into the corresponding property of its persisted
         * representation.
         *
         * @param TSource the type declaring the source property
         * @param TTarget the type declaring the target property
         * @param TProperty the shared value type of both properties
         * @param sourceProperty the property to match
         * @param targetProperty the property to read in the replacement expression
         * @return a replacer that maps [sourceProperty] accesses onto [targetProperty]
         */
        fun <TSource, TTarget, TProperty> property(
            sourceProperty: KProperty1<TSource, TProperty>,
            targetProperty: KProperty1<TTarget, TProperty>
        ): ExpressionReplacer {
            return property(sourceProperty) {
                PropertyExpression(
                    it.instance as Expression<Any, TTarget>,
                    targetProperty
                )
            }
        }
        
        /**
         * Creates a replacer that unwraps a domain value type to one of its properties.
         *
         * This combines two rewrites so that both sides of a query expression are translated
         * consistently:
         * - constant [TDomainValue] instances are replaced by the constant value of
         *   [valueProperty] (see [constantOfType]);
         * - accesses to [valueProperty] are replaced by the instance they are read from
         *   (see [property]), effectively removing the wrapper.
         *
         * A typical use is unwrapping a wrapper/identifier type around a scalar so that
         * comparisons operate directly on the underlying scalar value.
         *
         * @param TDomainValue the domain wrapper type to unwrap
         * @param TProperty the underlying value type exposed by [valueProperty]
         * @param valueProperty the property holding the underlying value of [TDomainValue]
         * @return a replacer that unwraps [TDomainValue] to its [valueProperty]
         */
        inline fun <reified TDomainValue, TProperty> domainValue(
            valueProperty: KProperty1<TDomainValue, TProperty>
        ): ExpressionReplacer {
            return PriorityExpressionReplacer(
                listOf(
                    constantOfType<TDomainValue> {
                        ConstantExpression<Any, TProperty>(
                            valueProperty.get(it)
                        )
                    },
                    property(valueProperty) {
                        it.instance
                    }
                )
            )
        }
    }
}
