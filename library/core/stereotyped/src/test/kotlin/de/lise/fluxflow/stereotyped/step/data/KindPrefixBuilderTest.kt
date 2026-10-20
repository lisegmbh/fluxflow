package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.PrefixStrategy
import de.lise.fluxflow.stereotyped.step.data.KindPrefixBuilder.Companion.and
import de.lise.fluxflow.stereotyped.step.data.KindPrefixBuilder.Companion.build
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KindPrefixBuilderTest {
    data class ExampleData(
        @Data val plainAnnotated: String,
        @Data(identifier = "customId") val customAnnotated: Int,
        val withoutAnnotation: Boolean,
        @Data(identifier = "") val blankIdentifierAnnotated: Long,
    )

    @Test
    fun `apply should return original name when prefix is empty regardless of strategy`() {
        // Arrange
        val builderPlain = KindPrefixBuilder(PrefixStrategy.Plain, "")
        val builderCamel = KindPrefixBuilder(PrefixStrategy.CamelCase, "")

        // Act / Assert
        assertThat(builderPlain.apply("value")).isEqualTo("value")
        assertThat(builderCamel.apply("value")).isEqualTo("value")
    }

    @Test
    fun `apply should concatenate prefix and name with Plain strategy`() {
        val builder = KindPrefixBuilder(PrefixStrategy.Plain, "foo")
        assertThat(builder.apply("bar")).isEqualTo("foobar")
    }

    @Test
    fun `apply should camelCase first letter of name with CamelCase strategy for multi char name`() {
        val builder = KindPrefixBuilder(PrefixStrategy.CamelCase, "pre")
        assertThat(builder.apply("value")).isEqualTo("preValue")
    }

    @Test
    fun `apply should uppercase single letter for CamelCase strategy`() {
        val builder = KindPrefixBuilder(PrefixStrategy.CamelCase, "x")
        assertThat(builder.apply("a")).isEqualTo("xA")
    }

    @Test
    fun `and should create new builder when receiver is null`() {
        // Arrange
        val import = Import(prefix = "foo", prefixStrategy = PrefixStrategy.Plain)

        // Act
        val result = (null as KindPrefixBuilder?).and(import)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.prefix).isEqualTo("foo")
        assertThat(result.strategy).isEqualTo(PrefixStrategy.Plain)
    }

    @Test
    fun `and should chain prefixes using previous strategy`() {
        // Arrange
        val first = KindPrefixBuilder(PrefixStrategy.CamelCase, "foo")
        val secondImport = Import(prefix = "bar", prefixStrategy = PrefixStrategy.Plain)

        // Act
        val chained = first.and(secondImport)!!

        // Assert
        // Previous strategy CamelCase turns "bar" into "Bar" -> prefix becomes fooBar
        assertThat(chained.prefix).isEqualTo("fooBar")
        // Strategy is replaced by the new annotation's strategy
        assertThat(chained.strategy).isEqualTo(PrefixStrategy.Plain)
    }

    @Test
    fun `and should allow multiple chaining with different strategies`() {
        // Arrange
        val import1 = Import(prefix = "alpha", prefixStrategy = PrefixStrategy.Plain)
        val import2 = Import(prefix = "beta", prefixStrategy = PrefixStrategy.CamelCase)
        val import3 = Import(prefix = "gamma", prefixStrategy = PrefixStrategy.Plain)

        // Act
        val after1 = (null as KindPrefixBuilder?).and(import1)!! // prefix alpha, strategy Plain
        val after2 = after1.and(import2)!! // prefix alphabeta, strategy CamelCase
        val after3 = after2.and(import3)!! // prefix alphabetaGamma, strategy Plain

        // Assert
        assertThat(after3.prefix).isEqualTo("alphabetaGamma")
        assertThat(after3.strategy).isEqualTo(PrefixStrategy.Plain)
    }

    @Test
    fun `build should return raw kind (property name) when builder is null and no Data identifier`() {
        val kind = (null as KindPrefixBuilder?).build(ExampleData::withoutAnnotation)
        assertThat(kind).isEqualTo(DataKind("withoutAnnotation"))
    }

    @Test
    fun `build should use @Data identifier when present`() {
        val kind = (null as KindPrefixBuilder?).build(ExampleData::customAnnotated)
        assertThat(kind).isEqualTo(DataKind("customId"))
    }

    @Test
    fun `build should fall back to property name when @Data identifier blank`() {
        val kind = (null as KindPrefixBuilder?).build(ExampleData::blankIdentifierAnnotated)
        assertThat(kind).isEqualTo(DataKind("blankIdentifierAnnotated"))
    }

    @Test
    fun `build should prefix raw kind when builder provided with Plain strategy`() {
        val builder = KindPrefixBuilder(PrefixStrategy.Plain, "pre")
        val kind = builder.build(ExampleData::withoutAnnotation)
        assertThat(kind).isEqualTo(DataKind("prewithoutAnnotation"))
    }

    @Test
    fun `build should prefix and camelCase raw kind when builder provided with CamelCase strategy`() {
        val builder = KindPrefixBuilder(PrefixStrategy.CamelCase, "pre")
        val kind = builder.build(ExampleData::withoutAnnotation)
        assertThat(kind).isEqualTo(DataKind("preWithoutAnnotation"))
    }

    @Test
    fun `build should not alter kind when builder prefix is empty`() {
        val builder = KindPrefixBuilder(PrefixStrategy.CamelCase, "")
        val kind = builder.build(ExampleData::withoutAnnotation)
        assertThat(kind).isEqualTo(DataKind("withoutAnnotation"))
    }

    @Test
    fun `build should apply combined chained prefix to raw kind`() {
        // Arrange
        val import1 = Import(prefix = "foo", prefixStrategy = PrefixStrategy.Plain)
        val import2 = Import(prefix = "bar", prefixStrategy = PrefixStrategy.CamelCase)
        val chained = (null as KindPrefixBuilder?).and(import1)?.and(import2)!!

        // Act
        val kind = chained.build(ExampleData::withoutAnnotation)

        // Assert
        // After chaining: first prefix = foo; then CamelCase prefix for second takes Plain.apply(foo, bar) => foobar, so chained.prefix = foobar, strategy CamelCase
        assertThat(chained.prefix).isEqualTo("foobar")
        assertThat(kind).isEqualTo(DataKind("foobarWithoutAnnotation"))
    }
}
