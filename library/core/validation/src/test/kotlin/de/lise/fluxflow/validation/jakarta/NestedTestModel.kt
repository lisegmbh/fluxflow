package de.lise.fluxflow.validation.jakarta

import jakarta.validation.constraints.NotBlank

class NestedTestModel {
    companion object {
        const val NumberOfValidationsForSimpleTestProp = 1
    }
    @NotBlank
    var simpleTestProp: String = ""
}