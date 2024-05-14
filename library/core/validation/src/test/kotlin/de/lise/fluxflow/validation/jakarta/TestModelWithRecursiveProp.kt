package de.lise.fluxflow.validation.jakarta

import jakarta.validation.Valid

class TestModelWithRecursiveProp {
    @Valid
    var someProp: TypeWithRecursiveProp? = null
}