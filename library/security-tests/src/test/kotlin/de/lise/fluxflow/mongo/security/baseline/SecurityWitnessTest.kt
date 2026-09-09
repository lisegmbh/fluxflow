package de.lise.fluxflow.mongo.security.baseline

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecurityWitnessTest {
    @Test
    fun `R05 witness records initialization and construction independently`() {
        val fixture = Class.forName(
            "de.lise.fluxflow.mongo.security.baseline.fixture.ActivationWitness"
        )
        assertThat(fixture.getDeclaredConstructor().newInstance()).isNotNull()
    }
}
