package de.lise.fluxflow.mongo.workflow

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.not
import de.fluxflow.flowquery.query.Query
import de.fluxflow.flowquery.query.sorting.Sort.Companion.desc
import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.UUID

private typealias MongoQuery = org.springframework.data.mongodb.core.query.Query

@MongoIntegrationTest
class WorkflowMongoPersistenceIT {
    @Autowired
    lateinit var workflowPersistence: WorkflowPersistence
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Test
    fun `find all should support filter, sorting and pagination`() {
        val result = workflowPersistence.findAll(
            Query.of {
                where {
                    (get(WorkflowData::model) as Expression<WorkflowData, TestModel>)
                        .get(TestModel::aStringProperty)
                        .isEqual("a")
                        .not()
                }.sort {
                    (get(WorkflowData::model) as Expression<WorkflowData, TestModel>)
                        .get(TestModel::aIntProperty)
                        .desc()
                }.paged(0, 2)
            }
        )

        assertThat(result.items).hasSize(2)
        assertThat(
            result.items.map {
                (it.model as TestModel).aStringProperty
            }
        ).containsExactly("d", "c")
    }

    @BeforeEach
    fun setup() {
        mongoTemplate.findAllAndRemove(
            MongoQuery(),
            WorkflowDocument::class.java
        )

        workflowPersistence.save(
            WorkflowData(
                id = UUID.randomUUID().toString(),
                model = TestModel(
                    aStringProperty = "a",
                    aIntProperty = 1,
                    nestedProperty = NestedTestModel(
                        anotherStringProperty = "z"
                    )
                )
            )
        )

        workflowPersistence.save(
            WorkflowData(
                id = UUID.randomUUID().toString(),
                model = TestModel(
                    aStringProperty = "b",
                    aIntProperty = 2,
                    nestedProperty = NestedTestModel(
                        anotherStringProperty = "y"
                    )
                )
            )
        )

        workflowPersistence.save(
            WorkflowData(
                id = UUID.randomUUID().toString(),
                model = TestModel(
                    aStringProperty = "c",
                    aIntProperty = 3,
                    nestedProperty = NestedTestModel(
                        anotherStringProperty = "x"
                    )
                )
            )
        )

        workflowPersistence.save(
            WorkflowData(
                id = UUID.randomUUID().toString(),
                model = TestModel(
                    aStringProperty = "d",
                    aIntProperty = 4,
                    nestedProperty = NestedTestModel(
                        anotherStringProperty = "w"
                    )
                )
            )
        )
    }

    data class TestModel(
        val aStringProperty: String,
        val aIntProperty: Int,
        val nestedProperty: NestedTestModel
    )

    data class NestedTestModel(
        val anotherStringProperty: String
    )
}