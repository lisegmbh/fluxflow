package de.lise.fluxflow.mongo.security.baseline

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecurityWitnessTest {
    @Test
    fun `R05 witness records initialization and construction independently`() {
        val firstLoader = WitnessClassLoader()
        val secondLoader = WitnessClassLoader()
        val firstType = exerciseWitness(firstLoader)
        val secondType = exerciseWitness(secondLoader)
        assertThat(firstType).isNotSameAs(secondType)
        assertThat(firstLoader.events).containsExactly("initialized", "constructed")
        assertThat(secondLoader.events).containsExactly("initialized", "constructed")
    }

    private fun exerciseWitness(loader: WitnessClassLoader): Class<*> {
        val name = "de.lise.fluxflow.mongo.security.baseline.fixture.ActivationWitness"
        val fixture = Class.forName(name, false, loader)
        assertThat(loader.events).isEmpty()
        Class.forName(name, true, loader)
        assertThat(loader.events).containsExactly("initialized")
        assertThat(fixture.getDeclaredConstructor().newInstance()).isNotNull()
        assertThat(loader.events).containsExactly("initialized", "constructed")
        return fixture
    }
}
