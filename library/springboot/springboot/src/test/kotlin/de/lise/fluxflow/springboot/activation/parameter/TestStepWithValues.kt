package de.lise.fluxflow.springboot.activation.parameter

import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.action.Action
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

@Step
class TestStepWithValues(
    var aStringValue: String? = null,
    var aStringValueWithNeedlessDefault: String? = null,
    var aStringValueWithDefault: String? = null,
    var aIntValue: Int? = null,
    var now: Instant? = null,
    var someValueIsEqual: Boolean? = null
) {
    @Action
    fun fetchValues(
        @Value("\${props.string}")
        aStringValue: String,
        @Value("\${props.string:default}")
        aStringValueWithNeedlessDefault: String,
        @Value("\${props.nonExistingString:i am a default}")
        aStringValueWithDefault: String,
        @Value("\${props.int}")
        aIntValue: Int,
        @Value("#{valueTestClock.instant()}")
        now: Instant,
        @Value("#{'\${props.someString}'.toLowerCase().equals('a value')}")
        someValueIsEqual: Boolean
    ) {
        this.aStringValue = aStringValue
        this.aStringValueWithNeedlessDefault = aStringValueWithNeedlessDefault
        this.aStringValueWithDefault = aStringValueWithDefault
        this.aIntValue = aIntValue
        this.now = now
        this.someValueIsEqual = someValueIsEqual
    }
}