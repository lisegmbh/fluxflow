package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.engine.security.ActivationResolverWitness
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class StepTypeResolverImplTest {
    @Test
    fun `M05 should resolve only registered step FQCNs and aliases`() {
        val fqcn = AllowedStep::class.qualifiedName!!
        val registry = registry(
            entry(TypeRole.STEP, fqcn, AllowedStep::class.java.name),
            entry(TypeRole.STEP, "allowed-step", AliasedStep::class.java.name),
        )
        val resolver = StepTypeResolverImpl(javaClass.classLoader, registry)

        assertThat(resolver.resolveType(StepKind(fqcn)))
            .isEqualTo(AllowedStep::class)
        assertThat(resolver.resolveType(StepKind("allowed-step")))
            .isEqualTo(AliasedStep::class)
    }

    @Test
    fun `M06 should reject a step registered only for another role without initializing it`() {
        val witnessName = ActivationResolverWitness.StepOtherRoleWitness::class.java.name
        clearWitness(witnessName)
        val registry = registry(
            entry(TypeRole.MODEL, witnessName, witnessName),
        )
        val resolver = StepTypeResolverImpl(javaClass.classLoader, registry)

        val failure = catchThrowable {
            resolver.resolveType(StepKind(witnessName))
        }

        assertThat(failure).isExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat(failure).extracting("role", "key")
            .containsExactly(TypeRole.STEP, witnessName)
        assertWitnessUntouched(witnessName)
    }

    @Test
    fun `M06 legacy constructor should reject an unregistered class without initializing it`() {
        val witnessName = ActivationResolverWitness.StepLegacyWitness::class.java.name
        clearWitness(witnessName)
        val resolver = StepTypeResolverImpl(javaClass.classLoader)

        val failure = catchThrowable {
            resolver.resolveType(StepKind(witnessName))
        }

        assertThat(failure).isExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat(failure).extracting("role", "key")
            .containsExactly(TypeRole.STEP, witnessName)
        assertWitnessUntouched(witnessName)
    }

    @Test
    fun `M07 should isolate resolver mappings from later mutations`() {
        val lateKind = StepKind("late-step")
        val mappings = mutableMapOf<StepKind, KClass<out Any>>(
            StepKind("allowed-step") to AllowedStep::class,
        )
        val resolver = StepTypeResolverImpl(javaClass.classLoader, mappings)

        mappings[lateKind] = AliasedStep::class

        assertThat(catchThrowable { resolver.resolveType(lateKind) })
            .isExactlyInstanceOf(UnknownTypeException::class.java)
    }

    private fun registry(vararg entries: TypeManifestEntry): TypeRegistry =
        TypeRegistry.create(javaClass.classLoader, entries.toList())

    private fun entry(role: TypeRole, key: String, binaryClassName: String) =
        TypeManifestEntry(role, key, binaryClassName, "core-engine test")

    private fun clearWitness(name: String) {
        System.clearProperty("$name.initialized")
        System.clearProperty("$name.constructed")
    }

    private fun assertWitnessUntouched(name: String) {
        assertThat(System.getProperty("$name.initialized")).isNull()
        assertThat(System.getProperty("$name.constructed")).isNull()
    }

    private class AllowedStep
    private class AliasedStep
}
