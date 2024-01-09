package de.lise.fluxflow.springboot.ioc

import de.lise.fluxflow.api.ioc.IocProvider
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass
import kotlin.reflect.KType

class SpringIocProvider(
    private val applicationContext: ApplicationContext
) : IocProvider {
    override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? {
        return try {
            applicationContext.getBean(type.java)
        } catch (e: NoSuchBeanDefinitionException) {
            null
        }
    }

    override fun provide(type: KType): Any? {
        val javaClass = (type.classifier as? KClass<*>)?.java
        return try {
            applicationContext.getBean(javaClass)
        } catch (e: NoSuchBeanDefinitionException) {
            null
        }
    }
}