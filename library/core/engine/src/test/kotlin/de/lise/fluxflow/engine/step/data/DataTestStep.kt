package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.stereotyped.step.data.Data

class DataTestStep(
    @Data
    @field:DataTestStepAnnotation("field")
    val someDataWithFieldMetadata: String = "value1",
    @Data
    @property:DataTestStepAnnotation("property")
    val someDataWithPropertyMetadata: String = "value2",
    @Data
    @get:DataTestStepAnnotation("getter")
    val someDataWithGetterMetadata: String = "value3"
)