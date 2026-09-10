package de.lise.fluxflow.mongo.flowquery.expression.compilation

import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegistrySubclassProviderTest {
    @Test
    fun `D10 exposes only concrete registered types assignable to the query type`() {
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(
                entry(TypeRole.MODEL, "first", FirstModel::class.java),
                entry(TypeRole.MODEL, "second", SecondModel::class.java),
                entry(TypeRole.VALUE, "other", OtherValue::class.java),
            ),
        )

        val provider = RegistrySubclassProvider(registry)

        assertThat(provider.findSubclasses(Model::class))
            .containsExactlyInAnyOrder(FirstModel::class.java, SecondModel::class.java)
        assertThat(provider.findSubclasses(FirstModel::class))
            .containsExactly(FirstModel::class.java)
        assertThat(provider.findSubclasses(UnregisteredModel::class)).isEmpty()
    }

    private fun entry(role: TypeRole, key: String, type: Class<*>): TypeManifestEntry =
        TypeManifestEntry(role, key, type.name, "RegistrySubclassProviderTest")

    private interface Model
    private data class FirstModel(val value: String) : Model
    private data class SecondModel(val value: String) : Model
    private data class UnregisteredModel(val value: String) : Model
    private data class OtherValue(val value: String)
}
