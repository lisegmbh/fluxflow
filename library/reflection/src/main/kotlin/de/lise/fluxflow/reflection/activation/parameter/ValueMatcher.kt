package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.reflection.ReflectionUtils
import kotlin.reflect.full.isSubclassOf

fun interface ValueMatcher<T> {
    fun matches(
        functionParam: FunctionParameter<*>,
        actualParameter: T
    ): Boolean


    fun <TSub : T> and(other: ValueMatcher<TSub>): ValueMatcher<TSub> {
        return ValueMatcher { fnParam, actualParameter ->
            matches(fnParam, actualParameter) && other.matches(fnParam, actualParameter)
        }
    }

    fun and(paramMatcher: ParamMatcher): ValueMatcher<T> {
        return ValueMatcher { fnParam, actualParameter ->
            matches(fnParam, actualParameter) && paramMatcher.matches(fnParam)
        }
    }

    fun <TSub : T> or(other: ValueMatcher<TSub>): ValueMatcher<TSub> {
        return ValueMatcher { fnParam, actualParameter ->
            matches(fnParam, actualParameter) || other.matches(fnParam, actualParameter)
        }
    }

    fun not(): ValueMatcher<T> {
        return ValueMatcher { fnParam, actualParameter ->
            !matches(fnParam, actualParameter)
        }
    }

    companion object {
        @JvmStatic
        fun <T> ignoreValue(paramMatcher: ParamMatcher): ValueMatcher<T> {
            return ValueMatcher { fnParam, _ ->
                paramMatcher.matches(fnParam)
            }
        }

        @JvmStatic
        fun <T> hasName(name: String): ValueMatcher<T> {
            return ParamMatcher.hasName(name).toValueMatcher()
        }

        @JvmStatic
        fun <T> valueIsNotNull(): ValueMatcher<T> {
            return ValueMatcher { _, actualParameter ->
                actualParameter != null
            }
        }

        @JvmStatic
        fun <T> canBeAssigned(): ValueMatcher<T> {
            return ValueMatcher { fnParam, actualParameter ->
                if (actualParameter == null) {
                    return@ValueMatcher fnParam.param.type.isMarkedNullable
                }

                return@ValueMatcher ReflectionUtils.getParameterClass(fnParam.param)?.let { formalClass ->
                    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Seems to be an IntelliJ bug
                    actualParameter!!::class.isSubclassOf(formalClass)
                } ?: false
            }
        }

    }
}