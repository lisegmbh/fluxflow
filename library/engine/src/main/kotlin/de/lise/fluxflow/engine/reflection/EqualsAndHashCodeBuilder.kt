package de.lise.fluxflow.engine.reflection

import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

typealias PropertyMap<TElement> = Map<KProperty1<TElement, *>, (element: TElement) -> Any?>

class EqualsAndHashCodeBuilder<TElement : Any>(
    private val type: KClass<TElement>,
    private val strictTypeCheck: Boolean = true,
    private val makeAccessible: Boolean = false
) {
    fun build(): EqualsAndHashCode<TElement> {
        val propertyGetters = buildPropertyGetters()
        val propertyEqualityChecker = buildPropertyChecker(propertyGetters)

        val equalityChecker: JavaEqualityChecker<TElement> = when(strictTypeCheck) {
            true -> JavaEqualityChecker { instance, other ->
                when(val castedOther = other?.let { type.safeCast(it) }) {
                    null -> false
                    else -> propertyEqualityChecker.checkIfEqual(instance, castedOther)
                }
            }
            else -> JavaEqualityChecker { instance, other ->
                when(other?.let { it::class }) {
                    type -> propertyEqualityChecker.checkIfEqual(instance, type.cast(other))
                    else -> false
                }
            }
        }

        val hashCodeComposer = buildHashCodeComposer(propertyGetters)

        return EqualsAndHashCode(
            equalityChecker,
            hashCodeComposer
        )
    }


    private fun buildPropertyGetters(): PropertyMap<TElement> {
        val relevantProperties = type.memberProperties
            .filter {
                it.visibility == KVisibility.PUBLIC
            }.mapNotNull {
                when(it.isAccessible) {
                    true -> it
                    false -> when(makeAccessible) {
                        true -> it.apply { it.isAccessible = true }
                        else -> null
                    }
                }
            }

        return relevantProperties.associateWith { property ->
            { instance ->
                property.get(instance)
            }
        }
    }

    private fun buildHashCodeComposer(
        propertyGetters: PropertyMap<TElement>
    ): HashComposer<TElement> {
        return propertyGetters.values.map { getter ->
            HashComposer<TElement> { instance ->
                getter(instance)?.hashCode() ?: 0
            }
        }.reduceOrNull { acc, hashComposer ->
            acc and hashComposer
        } ?: HashComposer { 0 }
    }

    private fun buildPropertyChecker(
        propertyGetters: PropertyMap<TElement>
    ): EqualityChecker<TElement, TElement?> {
        return propertyGetters.values.map { getter ->
            EqualityChecker<TElement, TElement?> { instance, other ->
                when(other) {
                    null -> false
                    else -> getter(instance) == getter(other)
                }
            }
        }.reduceOrNull { acc, equalityChecker ->
            acc and equalityChecker
        } ?: EqualityChecker.alwaysEqual()
    }
}