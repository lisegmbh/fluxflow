package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.reflection.ReflectionUtils
import de.lise.fluxflow.reflection.toKClass
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSuperclassOf

fun interface ParamMatcher {
    fun matches(functionParam: FunctionParameter<*>): Boolean


    fun and(other: ParamMatcher): ParamMatcher {
        return ParamMatcher {
            matches(it) && other.matches(it)
        }
    }

    fun or(other: ParamMatcher): ParamMatcher {
        return ParamMatcher {
            matches(it) || other.matches(it)
        }
    }

    fun not(): ParamMatcher {
        return ParamMatcher {
            !matches(it)
        }
    }

    fun <T> toValueMatcher(): ValueMatcher<T> {
        return ValueMatcher.ignoreValue(this)
    }

    companion object {
        @JvmStatic
        fun hasName(name: String): ParamMatcher {
            return ParamMatcher {
                it.param.name == name
            }
        }

        @JvmStatic
        fun hasNameContaining(value: String, ignoreCase: Boolean = true): ParamMatcher {
            return ParamMatcher {
                it.param.name?.contains(value, ignoreCase) ?: false
            }
        }

        @JvmStatic
        fun isFunctionInstance(): ParamMatcher {
            return ParamMatcher {
                it.param.kind == KParameter.Kind.INSTANCE
            }
        }

        @JvmStatic
        fun isAssignableFrom(actualType: KClass<*>): ParamMatcher {
            return ParamMatcher {
                val formalType = ReflectionUtils.getParameterClass(it.param)?: return@ParamMatcher false
                return@ParamMatcher formalType.isSuperclassOf(actualType)
            }
        }

        @JvmStatic
        fun isAssignableFrom(actualType: Type): ParamMatcher {
            return isAssignableFrom(actualType.toKClass()!!)
        }

        @JvmStatic
        fun isAtIndexAmongMatching(
            matcher: ParamMatcher,
            index: Int,
        ): ParamMatcher {
            return ParamMatcher { fnParam ->
                matcher.matches(fnParam) &&
                        fnParam.function.parameters.map { FunctionParameter(fnParam.function, it) }
                            .filter { matcher.matches(it) }
                            .indexOf(fnParam) == index
            }
        }
    }
}