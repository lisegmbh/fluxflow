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

    @Test
    fun `V01 enum witness stays dormant until constants are accessed`() {
        val loader = WitnessClassLoader()
        val fixture = Class.forName(ENUM_WITNESS_NAME, false, loader)

        assertThat(loader.events).isEmpty()
        assertThat(fixture.enumConstants.map { (it as Enum<*>).name }).containsExactly("SAFE")
        assertThat(loader.events).containsExactly("enum-initialized")
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
