package de.lise.fluxflow.reflection

import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.javaType

class ReflectionUtils private constructor() {
    companion object {

        @JvmStatic
        fun <TObject, TProp : Any> findReturnClass(
            prop: KProperty1<out TObject, TProp?>
        ): KClass<TProp> {
            @Suppress("UNCHECKED_CAST")
            return prop.returnType.classifier as KClass<TProp>
        }

        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun <TObject, TProp : Any> findReturnType(
            prop: KProperty1<out TObject, TProp?>
        ): Type {
            return prop.returnType.javaType
        }

        fun getParameterClass(
            parameter: KParameter
        ): KClass<*>? {
            return parameter.type.classifier as? KClass<*>
        }
    }
}