package de.lise.fluxflow.mongo.flowquery.expression.compilation

import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/** Uses the immutable type manifest as the complete query-time subtype inventory. */
class RegistrySubclassProvider(
    registry: TypeRegistry,
) : SubclassProvider, TypeAliasProvider {
    private val registrations = registry.entries
        .asSequence()
        .filter { it.role == TypeRole.MODEL || it.role == TypeRole.VALUE }
        .filterNot { it.type.java.isInterface || Modifier.isAbstract(it.type.java.modifiers) }
        .toList()

    private val registeredTypes = registrations.map { it.type.java }.toSet()

    override fun findSubclasses(type: KClass<*>): Set<Class<*>> =
        registeredTypes.filterTo(mutableSetOf()) { type.java.isAssignableFrom(it) }

    override fun findTypeAliases(type: KClass<*>): Set<String> = registrations
        .asSequence()
        .filter { type.java.isAssignableFrom(it.type.java) }
        .flatMap { sequenceOf(it.key, it.binaryClassName) }
        .toSortedSet()
}
