package de.lise.fluxflow.springboot.activation.parameter

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TestComponentWithValues(
    @Value("\${props.string}")
    val aStringValue: String,
    @Value("\${props.string:default}")
    val aStringValueWithNeedlessDefault: String,
    @Value("\${props.nonExistingString:i am a default}")
    val aStringValueWithDefault: String,
    @Value("\${props.int}")
    val aIntValue: Int,
    @Value("#{valueTestClock.instant()}")
    val now: Instant,
    @Value("#{'\${props.someString}'.toLowerCase().equals('a value')}")
    val someValueIsEqual: Boolean
)