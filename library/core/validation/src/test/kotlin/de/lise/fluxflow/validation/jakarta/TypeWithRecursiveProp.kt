package de.lise.fluxflow.validation.jakarta

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

class TypeWithRecursiveProp {
    companion object {
        const val NumberOfValidationsForRecursiveProp = 2
    }
    @Valid
    @NotNull
    var recursiveProp: TypeWithRecursiveProp? = null
}