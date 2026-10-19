package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.FluxFlowMongoTypeAliases
import de.lise.fluxflow.mongo.MongoDocumentTypePolicy
import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.bson.Document
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class MongoDocumentTypePolicyAliasRenderingTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(ProjectionBranch::class)
    fun `non-string aliases are rejected without rendering their value`(branch: ProjectionBranch) {
        val policy = policy()
        policy.validateProjection(branch.targetType, Document("_class", branch.validAlias))

        val sentinel = ToStringSentinel()
        val failure = catchThrowable {
            policy.validateProjection(branch.targetType, Document("_class", sentinel))
        }

        assertThat(failure).isExactlyInstanceOf(IllegalArgumentException::class.java)
        assertThat(requireNotNull(failure).message)
            .isEqualTo("Mongo type alias '<non-string>' ${branch.rejectionMessage}")
        assertThat(failure.suppressed.map(Throwable::message))
            .containsExactly("Invalid Mongo type at 'projection._class'")
        assertThat(sentinel.toStringCalls).isZero()

        val invalidAliasFailure = catchThrowable {
            policy.validateProjection(branch.targetType, Document("_class", "unregistered-alias"))
        }

        assertThat(invalidAliasFailure).isExactlyInstanceOf(IllegalArgumentException::class.java)
        assertThat(requireNotNull(invalidAliasFailure).message)
            .isEqualTo("Mongo type alias 'unregistered-alias' ${branch.rejectionMessage}")
        assertThat(invalidAliasFailure.suppressed.map(Throwable::message))
            .containsExactly("Invalid Mongo type at 'projection._class'")
    }

    private fun policy(): MongoDocumentTypePolicy {
        val registry = TypeRegistry.create(javaClass.classLoader, emptyList())
        val infrastructure = setOf(
            SimpleType::class.java,
            CollectionType::class.java,
            NullType::class.java,
            JvmTypeMapping::class.java,
            TypeRecordEntry::class.java,
            TypedRecords::class.java,
            DataDefinitionDocument::class.java,
        )
        val aliases = FluxFlowMongoTypeAliases(
            registry,
            setOf(WorkflowDocument::class.java, StepDocument::class.java, JobDocument::class.java,
                StepDefinitionDocument::class.java),
            infrastructure,
        )
        return MongoDocumentTypePolicy(
            aliases,
            isRegisteredJvmValueType = ValueTypeConverter(registry)::isRegistered,
        )
    }

    enum class ProjectionBranch(
        val targetType: Class<*>,
        val validAlias: String,
        val rejectionMessage: String,
    ) {
        INFRASTRUCTURE(
            SimpleType::class.java,
            SimpleType::class.java.name,
            "is not allowed in FluxFlow infrastructure metadata",
        ),
        TYPED_RECORDS(
            TypedRecords::class.java,
            TypedRecords::class.java.name,
            "is not a TypedRecords container",
        ),
        DATA_DEFINITION(
            DataDefinitionDocument::class.java,
            DataDefinitionDocument::class.java.name,
            "is not a step data definition",
        ),
    }

    private class ToStringSentinel : AbstractMap<String, Any?>() {
        var toStringCalls: Int = 0
            private set

        override val entries: Set<Map.Entry<String, Any?>> = emptySet()

        override fun toString(): String {
            toStringCalls++
            return "alias-rendered-$toStringCalls"
        }
    }
}
