package de.lise.fluxflow.mongo.generic

class NullType : TypeSpec {
    override fun assertType(value: Any?): Any? {
        return null // A null type always represents a null value
    }
}