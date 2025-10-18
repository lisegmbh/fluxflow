package de.fluxflow.flowquery.expression

class Root<T> : Expression<T, T> {
    override fun toText(): String {
        return "$"
    }

    override fun toString(): String {
        return toText()
    }

    override fun equals(other: Any?): Boolean {
        return other is Root<*>
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}