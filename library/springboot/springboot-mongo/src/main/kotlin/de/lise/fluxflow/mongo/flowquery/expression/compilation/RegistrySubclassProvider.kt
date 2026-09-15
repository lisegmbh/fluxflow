package de.lise.fluxflow.mongo.flowquery.expression.compilation

import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/** Uses the immutable type manifest as the complete query-time subtype inventory. */
class RegistrySubclassProvider(
    registry: TypeRegistry,
) : SubclassProvider {
    private val registeredTypes = registry.entries
        .asSequence()
        .filter { it.role == TypeRole.MODEL || it.role == TypeRole.VALUE }
        .map { it.type.java }
        .filterNot { it.isInterface || Modifier.isAbstract(it.modifiers) }
        .toSet()

    override fun findSubclasses(type: KClass<*>): Set<Class<*>> =
        registeredTypes.filterTo(mutableSetOf()) { type.java.isAssignableFrom(it) }
}
