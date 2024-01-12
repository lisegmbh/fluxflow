package de.lise.fluxflow.api.proxy

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import kotlin.reflect.KFunction
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaMethod

class KotlinElementMatchers private constructor(){
    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun KFunction<*>.elementMatcher(): ElementMatcher<in MethodDescription> {
            return ElementMatchers.named<MethodDescription?>(this.javaMethod?.name ?: this.name)
                .and(ElementMatchers.returns(this.javaMethod?.returnType ?: this.returnType.javaType as Class<*>))
                .and(ElementMatchers.takesArguments(
                    *this.javaMethod!!.parameters.map { it.type }.toTypedArray()
                ))
        }
    }
}