package de.lise.fluxflow.api.step.stateful.action

data class ActionKind(
    val value: String
) {
    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActionKind

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}
