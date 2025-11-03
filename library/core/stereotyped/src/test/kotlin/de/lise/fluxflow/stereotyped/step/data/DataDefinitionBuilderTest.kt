package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStep
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class DataDefinitionBuilderTest {
    private val listenerDefinitionBuilder = mock<DataListenerDefinitionBuilder> {}

    @Test
    fun `build should never return null`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val builder = dataDefinitionBuilder.buildDataDefinition(
            TestClass::class,
        )

        // Assert
        assertThat(builder).isNotNull
    }

    @Test
    fun `build should return a builder that creates an unmodifiable data definition for read only properties`() {
        // Arrange
        val instance = TestClass("hello")

        // Act
        val data = createDataObject(
            instance,
            TestClass::readOnlyStringProperty
        )

        // Assert
        assertThat(data).isNotInstanceOf(ModifiableData::class.java)
        assertThat(data.definition.isReadonly).isTrue()
    }

    @Test
    fun `build should return a builder that creates a modifiable data definition for read only properties`() {
        // Arrange
        val instance = ModifiableTestClass("hello")

        // Act
        val data = createDataObject(
            instance,
            ModifiableTestClass::modifiableStringProperty
        )

        // Assert
        assertThat(data).isInstanceOf(ModifiableData::class.java)
        assertThat(data.definition.isReadonly).isFalse()
    }

    @Test
    fun `build should return a builder that creates a data definition accessing the correct property`() {
        // Arrange
        val instance = TestClass("foo bar")
        val data = createDataObject(
            instance,
            TestClass::readOnlyStringProperty
        )

        // Act
        val value = data.get()

        // Assert
        assertThat(value).isEqualTo("foo bar")
    }

    @Test
    fun `build should return a builder that creates a data definition setting the correct property`() {
        // Arrange
        val instance = ModifiableTestClass("The answer is 42")
        val data = createDataObject(
            instance,
            ModifiableTestClass::modifiableStringProperty
        )
                as ModifiableData<String>

        // Act
        data.set("But what is the question?")

        // Assert
        assertThat(instance.modifiableStringProperty).isEqualTo("But what is the question?")
    }

    @Test
    fun `build should set isCalculatedValue to false if the property is backed by a field`() {
        // Arrange
        val instance = ModifiableTestClass("Foo")

        // Act
        val data = createDataObject(
            instance,
            ModifiableTestClass::modifiableStringProperty
        )
                as ModifiableData<String>

        // Assert 
        assertThat(data.definition.isCalculatedValue).isFalse()
    }

    @Test
    fun `build should set isCalculatedValue to true if explicitly specified`() {
        // Arrange
        val instance = ModifiableTestClassWithOverwrittenIsCalculated("Foo")

        // Act
        val data = createDataObject(
            instance,
            ModifiableTestClassWithOverwrittenIsCalculated::overwrittenProperty
        )
                as ModifiableData<String>

        // Assert 
        assertThat(data.definition.isCalculatedValue).isTrue()
    }

    @Test
    fun `build should set isCalculatedValue to true if there is no backing field`() {
        // Arrange
        val instance = TestClassWithCalculatedProperty()

        // Act
        val data = createDataObject(
            instance,
            TestClassWithCalculatedProperty::calculatedProperty
        )
                as ModifiableData<String>

        // Assert 
        assertThat(data.definition.isCalculatedValue).isTrue()
    }

    @Test
    fun `build should include imported data definitions`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val definitions = dataDefinitionBuilder.buildDataDefinition(TestWithImport::class)

        // Assert
        assertThat(
            definitions.map { it.kind }
        ).contains(
            DataKindInspector.getDataKind(SimpleImportableDataClass::name),
            DataKindInspector.getDataKind(SimpleImportableDataClass::modifiableProperty)
        )
    }

    @Test
    fun `build should keep regular data definitions, even if others are imported`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val definitions = dataDefinitionBuilder.buildDataDefinition(TestWithImport::class)

        // Assert
        assertThat(
            definitions.map { it.kind }
        ).contains(
            DataKindInspector.getDataKind(TestWithImport::regularDataDefinition)
        )
    }

    @Test
    fun `build should not recognize properties annotated with @Import as direct data definitions`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val definitions = dataDefinitionBuilder.buildDataDefinition(TestWithImport::class)

        // Assert
        assertThat(
            definitions.map { it.kind }
        ).doesNotContain(
            DataKindInspector.getDataKind(TestWithImport::importProperty)
        )
    }

    @Test
    fun `returned data definitions should be capable to get and set values`() {
        // Arrange
        val instance = TestWithImport(
            importProperty = SimpleImportableDataClass(
                "name",
                false
            ),
            regularDataDefinition = 42
        )

        // Act
        val data = createDataObject<Boolean>(
            instance,
            DataKindInspector.getDataKind(SimpleImportableDataClass::modifiableProperty)
        )
        val originalValue = data.get()
        (data as? ModifiableData<Boolean>)?.set(true)

        // Assert
        assertThat(originalValue).isFalse()
        assertThat(instance.importProperty.modifiableProperty).isTrue()
    }

    @Test
    fun `isDataProperty should return true for public properties, that do not represent a job`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val isDataProperty = dataDefinitionBuilder.isDataProperty(TestClass::readOnlyStringProperty)

        // Assert
        assertThat(isDataProperty).isTrue
    }

    @Test
    fun `isDataProperty should return false for private properties, that do not represent a job`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val isDataProperty = dataDefinitionBuilder.isDataProperty(
            TestClassWithPrivateProp::class.memberProperties
                .first { it.name == "privateProperty" }
        )

        // Assert
        assertThat(isDataProperty).isFalse
    }

    @Test
    fun `isDataProperty should return false for public properties, with a type that should be treated as job definition`() {
        // Arrange
        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )

        // Act
        val isDataProperty = dataDefinitionBuilder.isDataProperty(TestClassWithJobProp::testJob)

        // Assert
        assertThat(isDataProperty).isFalse
    }

    private fun <TObject : Any> mockReflectedStatefulStep(
        testInstance: TObject
    ): ReflectedStatefulStep {
        val step = mock<ReflectedStatefulStep> {
            on { instance } doReturn testInstance
        }
        return step
    }

    private fun <TProp> createDataObject(
        testInstance: Any,
        expectedKind: DataKind
    ): Data<TProp> {
        val step = mockReflectedStatefulStep(testInstance)

        val dataDefinitionBuilder = DataDefinitionBuilder(
            listenerDefinitionBuilder,
            mock<ValidationBuilder> {},
            mock<MetadataBuilder> {}
        )


        @Suppress("UNCHECKED_CAST")
        return dataDefinitionBuilder.buildDataDefinition(
            testInstance::class,
        )
            .first { expectedKind == it.kind }
            .createData(step) as Data<TProp>
    }

    private fun <TObject : Any, TProp> createDataObject(
        testInstance: TObject,
        prop: KProperty1<out TObject, TProp>,
    ): Data<TProp> {
        return createDataObject(
            testInstance,
            DataKindInspector.getDataKind(prop),
        )
    }

    class TestClass(
        val readOnlyStringProperty: String,
    )

    class ModifiableTestClass(
        var modifiableStringProperty: String,
    )

    class ModifiableTestClassWithOverwrittenIsCalculated(
        @de.lise.fluxflow.stereotyped.step.data.Data(persistenceType = PersistenceType.Calculated)
        var overwrittenProperty: String,
    )

    class TestClassWithCalculatedProperty {
        var calculatedProperty: String
            get() = "Hellow World"
            set(value) {}
    }

    class TestClassWithPrivateProp(
        private val privateProperty: Int,
    )

    @Job
    class TestJob
    class TestClassWithJobProp(
        val testJob: TestJob,
    )

    class TestWithImport(
        @Import
        val importProperty: SimpleImportableDataClass,
        val regularDataDefinition: Int
    )

    data class SimpleImportableDataClass(
        @de.lise.fluxflow.stereotyped.step.data.Data
        val name: String,
        var modifiableProperty: Boolean
    )
}