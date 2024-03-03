package de.lise.fluxflow.mongo.generic

class CollectionType(
    val collectionType: TypeSpec,
    val componentTypes: List<TypeSpec>
) : TypeSpec {
    override fun assertType(value: Any?): Any? {
        val elements = (value as Collection<*>).mapIndexed { index, element ->
            componentTypes[index].assertType(element)
        }
        return collectionType.assertType(elements)
    }
}