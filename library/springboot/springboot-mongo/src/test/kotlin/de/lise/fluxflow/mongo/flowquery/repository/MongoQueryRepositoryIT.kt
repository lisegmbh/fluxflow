package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.contains
import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.containsElementThat
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.and
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.not
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.contains
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.endsWith
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.startsWith
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.asType
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType
import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@MongoIntegrationTest
class MongoQueryRepositoryIT {
    @Autowired
    lateinit var template: MongoTemplate

    @Autowired
    internal lateinit var mongoQueryTranslator: MongoQueryTranslator

    val repo: MongoFlowQueryRepository<TestDocument> by lazy {
        MongoFlowQueryRepository(
            TestDocument::class,
            mongoQueryTranslator,
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
            FlowQuery.Companion.of()
        ).items

        // Assert
        assertThat(result).hasSize(testDocuments.size)
    }

    @Test
    fun `find with equals filter should support return matching documents`() {
        // Act
        val result = repo.find {
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
        val result = repo.find {
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

    @Test
    fun `find should support contains expressions on collections`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::collectionProp).contains(
                    NestedTestDocument(
                        anIntProperty = 2
                    )
                )
            }
        }

        // Assert
        assertThat(
            result.someStringProp
        ).isEqualTo("c")
    }

    @Test
    fun `find should support negated contains expressions on collections`() {
        // Act
        val result = repo.find {
            where {
                get(TestDocument::collectionProp).contains(
                    NestedTestDocument(
                        anIntProperty = 2
                    )
                ).not()
            }
        }.items

        // Assert
        assertThat(
            result.map { it.someStringProp }
        ).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun `find should support comparisons on instants`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::someInstant)
                    .isGreaterThan(Instant.now().minus(1, ChronoUnit.MINUTES))
            }
        }

        // Assert
        assertThat(
            result.someStringProp
        ).isEqualTo("b")
    }

    @Test
    fun `find should support filtering on boolean properties`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::aBooleanProperty)
            }
        }

        // Assert
        assertThat(result.someStringProp).isEqualTo("a")
    }

    @Test
    fun `find should support filtering on boolean properties combined with other filters`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::someStringProp).isEqual("a").and {
                    get(TestDocument::aBooleanProperty)
                }
            }
        }

        // Assert
        assertThat(result.someStringProp).isEqualTo("a")
    }

    @Test
    fun `find should support filtering on types`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::anAnyProperty).isType(NestedTestDocument::class)
            }
        }

        // Assert
        assertThat(result.anAnyProperty).isInstanceOf(NestedTestDocument::class.java)
    }

    @Test
    fun `find should support filtering on types supporting inheritance`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::anAnyProperty).isType(NestedDocument::class)
            }
        }

        // Assert
        assertThat(result.anAnyProperty).isInstanceOf(NestedTestDocument::class.java)
    }

    @Test
    fun `find should support filtering on casted properties`() {
        // Act
        val result = repo.findSingle {
            where {
                get(TestDocument::anAnyProperty)
                    .asType(NestedTestDocument::class)
                    .get(NestedTestDocument::anIntProperty)
                    .isGreaterThan(2)
            }
        }

        // Assert
        assertThat((result.anAnyProperty as NestedTestDocument).anIntProperty).isEqualTo(3)
    }
    
    @Test
    fun `find should support filtering on nullable properties`() {
        // Act
        val result = repo.findSingle { 
            where { 
                get(TestDocument::nullableNestedProperty)
                    .get(NestedTestDocument::anIntProperty)
                    .isGreaterThan(2)
            }
        }
        
        // Assert
        assertThat(result.nullableNestedProperty?.anIntProperty).isEqualTo(4)
    }

    private val testInstant = Instant.now()

    private val testDocuments = listOf(
        TestDocument(
            id = UUID.randomUUID(),
            someStringProp = "a",
            anotherStringProp = "z",
            longStringProperty = "efg",
            aBooleanProperty = true,
            nestedProperty = NestedTestDocument(
                anIntProperty = 1
            )
        ),
        TestDocument(
            id = UUID.randomUUID(),
            someStringProp = "b",
            anotherStringProp = "y",
            longStringProperty = "cde",
            aBooleanProperty = false,
            anAnyProperty = NestedTestDocument(
                anIntProperty = 3
            ),
            nestedProperty = NestedTestDocument(
                anIntProperty = 2,
                someInstant = testInstant
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
            aBooleanProperty = null,
            nestedProperty = NestedTestDocument(
                anIntProperty = 3
            ),
            nullableNestedProperty = NestedTestDocument(
                anIntProperty = 4                
            )
        )
    )

    data class TestDocument(
        val id: UUID,
        val someStringProp: String,
        val anotherStringProp: String,
        val longStringProperty: String,
        val aBooleanProperty: Boolean?,
        val anAnyProperty: Any? = null,
        val collectionProp: List<NestedTestDocument> = emptyList(),
        val nestedProperty: NestedTestDocument,
        val nullableNestedProperty: NestedTestDocument? = null,
    )

    data class NestedTestDocument(
        val anIntProperty: Int,
        val someInstant: Instant? = null
    ) : NestedDocument

    internal interface NestedDocument
}