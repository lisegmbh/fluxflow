package de.lise.fluxflow.springboot.ioc

import de.lise.fluxflow.api.ioc.IocProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass
import kotlin.reflect.KType

class SpringIocProvider(
    private val applicationContext: ApplicationContext
) : IocProvider {
    override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? {
        return provide(type.java)
    }

    override fun provide(type: KType): Any? {
        val javaClass = (type.classifier as? KClass<*>)?.java
        return provide(javaClass as Class<*>)
    }

    private fun <TDependency : Any> provide(clazz: Class<TDependency>): TDependency? {
        return try {
            applicationContext.getBean(clazz)
        } catch (e: NoUniqueBeanDefinitionException) {
            throw e
        } catch (e: NoSuchBeanDefinitionException) {
            Logger.debug(
                "Could not find a bean of type '{}' in the application context.",
                clazz.simpleName,
                e
            )
            null
        }
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(SpringIocProvider::class.java)!!
    }
}