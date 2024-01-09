package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import de.lise.fluxflow.mongo.step.StepDocument
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate

class MigrateMetadataTypesMapBootstrapAction(mongoTemplate: MongoTemplate) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        ensureCollection<StepDocument>().updateMany(
            Filters.eq("metadataTypeMap", null),
            listOf(
                Updates.set(
                    "metadataTypeMap",
                    Document(mapOf(
                        "\$objectToArray" to "\$metadataTypes"
                    ))
                ),
                Updates.set(
                    "metadataTypeMap",
                    Document(mapOf(
                        "\$map" to mapOf(
                            "input" to "\$metadataTypeMap",
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
                    ))
                ),
                Updates.set(
                    "metadataTypeMap",
                    Document(mapOf(
                        "\$arrayToObject" to "\$metadataTypeMap"
                    ))
                )
            )
        )
    }

}