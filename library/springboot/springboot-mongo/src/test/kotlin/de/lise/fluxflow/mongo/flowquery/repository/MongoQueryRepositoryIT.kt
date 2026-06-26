package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.contains
import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.containsElementThat
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isAnyOf
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isEqual
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isGreaterThan
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isLessThan
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.and
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.not
import de.fluxflow.flowquery.expression.ExpressionExtensions.Maps.get
import de.fluxflow.flowquery.expression.ExpressionExtensions.Maps.hasKey
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.contains
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.endsWith
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.startsWith
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.asType
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType
import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.fluxflow.flowquery.repository.NonProjectingRepository.Companion.find
import de.fluxflow.flowquery.repository.NonProjectingRepository.Companion.findSingle
import de.fluxflow.flowquery.repository.ProjectingRepository.Companion.find
import de.fluxflow.flowquery.repository.ProjectingRepository.Companion.findSingle
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
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

    private val repo: MongoFlowQueryRepository<TestDocument> by lazy {
        MongoFlowQueryRepository(
            rootType = TestDocument::class.java,
            translator = mongoQueryTranslator,
            executor = MongoExecutor(
                template,
                TestDocument::class.java
            )
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
        val result = repo.find(FlowQuery.of()).items

        // Assert
        assertThat(result).hasSize(testDocuments.size)
    }

    @Test
    fun `find with equals filter should return matching documents`() {
        val result = repo.find {
            where {
                get(TestDocument::someStringProp).isEqual("a")
            }
        }.items

        assertThat(result).hasSize(1)
        assertThat(result.first().someStringProp).isEqualTo("a")
    }

    @Test
    fun `find with id filter should transform valid object ids`() {
        val result = repo.find {
            where {
                get(TestDocument::id).isEqual(knownObjectId.toHexString())
            }
        }.items

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(knownObjectId.toHexString())
    }

    @Test
    fun `find with id filter should not transform invalid object ids`() {
        val result = repo.find {
            where {
                get(TestDocument::id).isEqual(knownUuid.toString())
            }
        }.items

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(knownUuid.toString())
    }

    @Test
    fun `find with id filter should support mixed UUID and ObjectIds`() {
        val result = repo.find {
            where {
                get(TestDocument::id).isAnyOf(
                    knownUuid.toString(),
                    knownObjectId.toHexString()
                )
            }
        }.items

        assertThat(result).hasSize(2)
        assertThat(result.singleOrNull { it.id == knownUuid.toString() }).isNotNull()
        assertThat(result.singleOrNull { it.id == knownObjectId.toHexString() }).isNotNull()
    }

    @Test
    fun `find with id filter should transform valid object ids, even if nested`() {
        val result = repo.find {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::id)
                    .isEqual(knownNestedObjectId.toHexString())
            }
        }.items

        assertThat(result).hasSize(1)
        assertThat(result.first().nestedProperty.id).isEqualTo(knownNestedObjectId.toHexString())
    }

    @Test
    fun `find with id filter should not transform invalid object ids, even if nested`() {
        val result = repo.find {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::id)
                    .isEqual(knownNestedUuid.toString())
            }
        }.items

        assertThat(result).hasSize(1)
        assertThat(result.first().nestedProperty.id).isEqualTo(knownNestedUuid.toString())
    }

    @Test
    fun `find with id filter should support mixed UUID and ObjectIds, even if nested`() {
        val result = repo.find {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::id)
                    .isAnyOf(
                        knownNestedUuid.toString(),
                        knownNestedObjectId.toHexString()
                    )
            }
        }.items

        assertThat(result).hasSize(2)
        assertThat(result.singleOrNull { it.nestedProperty.id == knownNestedUuid.toString() }).isNotNull()
        assertThat(result.singleOrNull { it.nestedProperty.id == knownNestedObjectId.toHexString() }).isNotNull()
    }

    @Test
    fun `find with lessThan filter should return matching documents`() {
        val result = repo.find {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::anIntProperty)
                    .isLessThan(3)
            }
        }.items

        assertThat(result).hasSize(2)
        assertThat(result.map { it.nestedProperty.anIntProperty }).containsExactly(
            1,
            2
        )
    }

    @Test
    fun `find should apply the specified sorting`() {
        val result = repo.find {
            sort {
                get(TestDocument::anotherStringProp).asc()
            }
        }

        assertThat(result.items).hasSize(testDocuments.size)
        assertThat(result.items.map { it.anotherStringProp }).containsExactly(
            "x",
            "y",
            "z"
        )
    }

    @Test
    fun `find should apply filter and sorting`() {
        val result = repo.find {
            where {
                get(TestDocument::someStringProp).isAnyOf(
                    "a",
                    "b"
                )
            }.sort {
                get(TestDocument::anotherStringProp).asc()
            }
        }.items

        assertThat(result).hasSize(2)
        assertThat(result.map { it.anotherStringProp }).containsExactly(
            "y",
            "z"
        )
    }

    @Test
    fun `find should support projection`() {
        val result = repo.find(NestedTestDocument::class) {
            where {
                get(TestDocument::someStringProp).isAnyOf(
                    "b",
                    "c"
                )
            }.project {
                get(TestDocument::nestedProperty)
            }.where {
                get(NestedTestDocument::anIntProperty).isEqual(3)
            }
        }

        assertThat(result).hasSize(1)
        assertThat(result.first().anIntProperty).isEqualTo(3)
    }

    @Test
    fun `find should support startsWith`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::longStringProperty).startsWith("ab")
            }
        }

        assertThat(result.longStringProperty).isEqualTo("abc")
    }

    @Test
    fun `find should support endsWith`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::longStringProperty).endsWith("de")
            }
        }

        assertThat(result.longStringProperty).isEqualTo("cde")
    }

    @Test
    fun `find should support contains on strings`() {
        val result = repo.find {
            where {
                get(TestDocument::longStringProperty).contains("c")
            }
        }

        assertThat(result.items.map { it.longStringProperty })
            .containsExactlyInAnyOrder(
                "abc",
                "cde"
            )
    }

    @Test
    fun `find should support containsElementThat`() {
        val result = repo.find {
            where {
                get(TestDocument::collectionProp).containsElementThat {
                    get(NestedTestDocument::anIntProperty).isGreaterThan(1)
                }
            }
        }

        assertThat(result.items.map { it.someStringProp })
            .containsExactlyInAnyOrder("c")
    }

    @Test
    fun `find should support contains on collections`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::collectionProp).contains(
                    NestedTestDocument(anIntProperty = 2)
                )
            }
        }

        assertThat(result.someStringProp).isEqualTo("c")
    }

    @Test
    fun `find should support negated contains on collections`() {
        val result = repo.find {
            where {
                get(TestDocument::collectionProp).contains(
                    NestedTestDocument(anIntProperty = 2)
                ).not()
            }
        }.items

        assertThat(result.map { it.someStringProp })
            .containsExactlyInAnyOrder(
                "a",
                "b"
            )
    }

    @Test
    fun `find should support comparisons on instants`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::nestedProperty)
                    .get(NestedTestDocument::someInstant)
                    .isGreaterThan(
                        Instant.now().minus(
                            1,
                            ChronoUnit.MINUTES
                        )
                    )
            }
        }

        assertThat(result.someStringProp).isEqualTo("b")
    }

    @Test
    fun `find should support boolean filters`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::aBooleanProperty)
            }
        }

        assertThat(result.someStringProp).isEqualTo("a")
    }

    @Test
    fun `find should support boolean filters combined with others`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::someStringProp).isEqual("a").and {
                    get(TestDocument::aBooleanProperty)
                }
            }
        }

        assertThat(result.someStringProp).isEqualTo("a")
    }

    @Test
    fun `find should support type filters`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::anAnyProperty).isType(NestedTestDocument::class)
            }
        }

        assertThat(result.anAnyProperty).isInstanceOf(NestedTestDocument::class.java)
    }

    @Test
    fun `find should support type filters with inheritance`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::anAnyProperty).isType(NestedDocument::class)
            }
        }

        assertThat(result.anAnyProperty).isInstanceOf(NestedTestDocument::class.java)
    }

    @Test
    fun `find should support casted property filters`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::anAnyProperty)
                    .asType(NestedTestDocument::class)
                    .get(NestedTestDocument::anIntProperty)
                    .isGreaterThan(2)
            }
        }

        assertThat((result.anAnyProperty as NestedTestDocument).anIntProperty).isEqualTo(3)
    }

    @Test
    fun `find should support nullable property filters`() {
        val result = repo.findSingle {
            where {
                get(TestDocument::nullableNestedProperty)
                    .get(NestedTestDocument::anIntProperty)
                    .isGreaterThan(2)
            }
        }

        assertThat(result.nullableNestedProperty?.anIntProperty).isEqualTo(4)
    }
    
    @Test
    fun `find should support filtering on map entries`() {
        val result = repo.findSingle(Map::class) { 
            where { 
                get(TestDocument::mapProperty)["testKey1"]
                    .asType(Int::class)
                    .isGreaterThan(0)
            }.project {
                get(TestDocument::mapProperty)
            }
        } as Map<String, Any?>
        assertThat(result["testKey1"] as Int).isGreaterThan(0)
    }
    
    @Test
    fun `find should support filtering for the presence of map keys`() {
        val result = repo.find { 
            where {
                get(TestDocument::mapProperty)
                    .hasKey("testKey1")
            }
        }
        
        assertThat(result.items).hasSize(1)
        assertThat(result.items).allMatch { 
            it.mapProperty.containsKey("testKey1")
        }
    }

    @Test
    fun `find should support filtering for the absence of map keys`() {
        val result1 = repo.find {
            where {
                get(TestDocument::mapProperty)
                    .hasKey("testKey1")
                    .not()
            }
        }
        assertThat(result1.items).hasSize(2)
        assertThat(result1.items).noneMatch {
            it.mapProperty.containsKey("testKey1")
        }

        val result2 = repo.find {
            where {
                get(TestDocument::mapProperty)
                    .hasKey("nonExisting")
                    .not()
            }
        }
        assertThat(result2.items).hasSize(3)
        assertThat(result2.items).noneMatch {
            it.mapProperty.containsKey("nonExisting")
        }
    }
    

    // -------------------------------------------------------------------------
    // Test Data
    // -------------------------------------------------------------------------

    private val testInstant = Instant.now()

    private val knownObjectId = ObjectId.get()
    private val knownNestedObjectId = ObjectId.get()
    private val knownUuid = UUID.randomUUID()
    private val knownNestedUuid = UUID.randomUUID()
    private val testDocuments = listOf(
        TestDocument(
            id = knownObjectId.toHexString(),
            someStringProp = "a",
            anotherStringProp = "z",
            longStringProperty = "efg",
            aBooleanProperty = true,
            nestedProperty = NestedTestDocument(
                anIntProperty = 1,
                id = knownNestedObjectId.toHexString()
            )
        ),
        TestDocument(
            id = knownUuid.toString(),
            someStringProp = "b",
            anotherStringProp = "y",
            longStringProperty = "cde",
            aBooleanProperty = false,
            anAnyProperty = NestedTestDocument(anIntProperty = 3),
            nestedProperty = NestedTestDocument(
                anIntProperty = 2,
                someInstant = testInstant,
                id = knownNestedUuid.toString()
            )
        ),
        TestDocument(
            id = UUID.randomUUID().toString(),
            someStringProp = "c",
            anotherStringProp = "x",
            longStringProperty = "abc",
            collectionProp = listOf(
                NestedTestDocument(anIntProperty = 1),
                NestedTestDocument(anIntProperty = 2),
                NestedTestDocument(anIntProperty = 3)
            ),
            aBooleanProperty = null,
            nestedProperty = NestedTestDocument(anIntProperty = 3),
            nullableNestedProperty = NestedTestDocument(anIntProperty = 4),
            mapProperty = mapOf(
                "testKey1" to 4,
                "testKey2" to "Hello",
                "testKey3" to null,
            )
        )
    )

    data class TestDocument(
        val id: String,
        val someStringProp: String,
        val anotherStringProp: String,
        val longStringProperty: String,
        val aBooleanProperty: Boolean?,
        val anAnyProperty: Any? = null,
        val collectionProp: List<NestedTestDocument> = emptyList(),
        val nestedProperty: NestedTestDocument,
        val nullableNestedProperty: NestedTestDocument? = null,
        val mapProperty: Map<String, Any?> = emptyMap(),
    )

    data class NestedTestDocument(
        val anIntProperty: Int,
        val someInstant: Instant? = null,
        val id: String? = null,
    ) : NestedDocument

    internal interface NestedDocument
}
