package de.lise.fluxflow.validation.jakarta

import jakarta.validation.Valid

class TestModel {
    @Valid
    var testObject: NestedTestModel = NestedTestModel()

    @Valid
    var testObjects: List<NestedTestModel> = listOf(NestedTestModel())
}