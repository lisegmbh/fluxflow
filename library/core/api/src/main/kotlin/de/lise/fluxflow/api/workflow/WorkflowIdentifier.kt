package de.lise.fluxflow.api.workflow

data class WorkflowIdentifier(val value: String) {
    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkflowIdentifier

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
