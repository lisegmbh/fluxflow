package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import de.lise.fluxflow.mongo.step.StepDocument
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate

private const val DATA_TYPE_MAP = "dataTypeMap"

class MigrateDataTypesMapBootstrapAction(
    mongoTemplate: MongoTemplate
) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        ensureCollection<StepDocument>().updateMany(
            Filters.and(
                Filters.or(
                    Filters.exists(DATA_TYPE_MAP, false),
                    Filters.eq(DATA_TYPE_MAP, null),
                ),
                Filters.exists("dataEntries", false)
            ),
            listOf(
                Updates.set(
                    DATA_TYPE_MAP,
                    Document(
                        mapOf(
                            "\$objectToArray" to "\$dataTypes"
                        )
                    )
                ),
                Updates.set(
                    DATA_TYPE_MAP,
                    Document(
                        mapOf(
                            "\$map" to mapOf(
                                "input" to "\$dataTypeMap",
                                "as" to "p",
                                "in" to mapOf(
                                    "k" to "\$\$p.k",
                                    "v" to mapOf(
                                        "\$cond" to mapOf(
                                            "if" to mapOf(
                                                "\$eq" to listOf(
                                                    "\$\$p.v",
                                                    null
                                                )
                                            ),
                                            "then" to mapOf(
                                                "_class" to "de.lise.fluxflow.mongo.generic.NullType"
                                            ),
                                            "else" to mapOf(
                                                "_class" to "de.lise.fluxflow.mongo.generic.SimpleType",
                                                "typeName" to "\$\$p.v"
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                Updates.set(
                    DATA_TYPE_MAP,
                    Document(
                        mapOf(
                            "\$arrayToObject" to "\$dataTypeMap"
                        )
                    )
                )
            )
        )
    }
}