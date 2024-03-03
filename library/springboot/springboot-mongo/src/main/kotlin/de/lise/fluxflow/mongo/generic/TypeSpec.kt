package de.lise.fluxflow.mongo.generic

interface TypeSpec {
    fun assertType(value: Any?): Any?

    companion object {
        fun fromValue(value: Any?): TypeSpec {
            if (value == null) {
                return NullType()
            }

            if (value !is Map<*,*> && value is Collection<*>) {
                return CollectionType(
                    SimpleType(value::class),
                    value.map { fromValue(it) }
                )
            }

            return SimpleType(value::class.java.canonicalName)
        }
    }
}