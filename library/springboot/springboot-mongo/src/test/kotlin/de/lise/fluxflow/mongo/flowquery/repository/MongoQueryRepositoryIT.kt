package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.containsElementThat
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.contains
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.endsWith
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.startsWith
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.mongo.flowquery.MongoCompiler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.*

@MongoIntegrationTest
class MongoQueryRepositoryIT {
    @Autowired
    lateinit var template: MongoTemplate

    val repo: MongoQueryRepository<TestDocument> by lazy {
        MongoQueryRepository(
            TestDocument::class,
            MongoCompiler(),
            template
        )
    }

    @BeforeEach
    fun setupRepo() {
        template.findAllAndRemove(
            Query(),
            TestDocument::class.java
        )
        template.insertAll(testDocuments)
    }

    @Test
    fun `find with an empty query should return all documents`() {
        // Act
        val result = repo.find(
            de.fluxflow.flowquery.query.Query.Companion.of()
        ).items

        // Assert
        assertThat(result).hasSize(testDocuments.size)
    }

    @Test
    fun `find with equals filter should support return matching documents`() {
        // Act
        val result = repo.find  {
            where {
                get(TestDocument::someStringProp).isEqual("a")
            }
        }.items

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result.first().someStringProp).isEqualTo("a")
    }

    @Test
    fun `find with lessThan filter should support return matching documents`() {
        // Act
        val result = repo.find  {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::anIntProperty)
                    .isLessThan(3)
            }
        }.items

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result.map { it.nestedProperty.anIntProperty }).containsExactly(1, 2)
    }

    @Test
    fun `find should apply the specified sorting`() {
        // Act
        val result = repo.find {
            sort {
                get(TestDocument::anotherStringProp).asc()
            }
        }

        // Assert
        assertThat(result.items).hasSize(testDocuments.size)
        assertThat(result.pageSize)
        assertThat(
            result.items.map { it.anotherStringProp }
        ).containsExactly("x", "y", "z")
    }

    @Test
    fun `find should apply the specified filter and sorting`() {
        // Act
        val result = repo.find {
            where {
                get(TestDocument::someStringProp).isAnyOf("a", "b")
            }.sort {
                get(TestDocument::anotherStringProp).asc()
            }
        }.items

        // Assert
        assertThat(result).hasSize(2)
        assertThat(
            result.map { it.anotherStringProp }
        ).containsExactly("y", "z")
    }

    @Test
    fun `find should support projection`() {
        // Act
        val result = repo.find(NestedTestDocument::class) {
            where {
                get(TestDocument::someStringProp).isAnyOf("b", "c")
            }.project {
                get(TestDocument::nestedProperty)
            }.where {
                get(NestedTestDocument::anIntProperty).isEqual(3)
            }
        }

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result.first().anIntProperty).isEqualTo(3)
    }

    @Test
    fun `find should support startsWith expressions`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::longStringProperty).startsWith("ab")
            }
        }

        // Assert
        assertThat(result.longStringProperty).isEqualTo("abc")
    }

    @Test
    fun `find should support endsWith expressions`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::longStringProperty).endsWith("de")
            }
        }

        // Assert
        assertThat(result.longStringProperty).isEqualTo("cde")
    }

    @Test
    fun `find should support contains expressions`() {
        // Act
        val result = repo.find {
            where {
                get(TestDocument::longStringProperty).contains("c")
            }
        }

        // Assert
        assertThat(
            result.items.map { it.longStringProperty }
        ).containsExactlyInAnyOrder("abc", "cde")
    }

    @Test
    fun `find should support containsElementThat expressions`() {
        // Act
        val result = repo.find {
            where {
                get(TestDocument::collectionProp).containsElementThat {
                    get(NestedTestDocument::anIntProperty)
                        .isGreaterThan(1)
                }
            }
        }

        // Assert
        assertThat(
            result.items.map { it.someStringProp }
        ).containsExactlyInAnyOrder("c")
    }

    private val testDocuments = listOf(
        TestDocument(
            id = UUID.randomUUID(),
            someStringProp = "a",
            anotherStringProp = "z",
            longStringProperty = "efg",
            nestedProperty = NestedTestDocument(
                anIntProperty = 1
            )
        ),
        TestDocument(
            id = UUID.randomUUID(),
            someStringProp = "b",
            anotherStringProp = "y",
            longStringProperty = "cde",
            nestedProperty = NestedTestDocument(
                anIntProperty = 2
            )
        ),
        TestDocument(
            id = UUID.randomUUID(),
            someStringProp = "c",
            anotherStringProp = "x",
            longStringProperty = "abc",
            collectionProp = listOf(
                NestedTestDocument(
                    anIntProperty = 1
                ),
                NestedTestDocument(
                    anIntProperty = 2
                ),
                NestedTestDocument(
                    anIntProperty = 3
                )
            ),
            nestedProperty = NestedTestDocument(
                anIntProperty = 3
            )
        )
    )

    data class TestDocument(
        val id: UUID,
        val someStringProp: String,
        val anotherStringProp: String,
        val longStringProperty: String,
        val collectionProp: List<NestedTestDocument> = emptyList(),
        val nestedProperty: NestedTestDocument,
    )

    data class NestedTestDocument(
        val anIntProperty: Int,
    )
}