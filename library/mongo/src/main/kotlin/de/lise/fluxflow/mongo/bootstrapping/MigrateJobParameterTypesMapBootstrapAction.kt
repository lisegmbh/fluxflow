package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import de.lise.fluxflow.mongo.job.JobDocument
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate

class MigrateJobParameterTypesMapBootstrapAction(mongoTemplate: MongoTemplate) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        ensureCollection<JobDocument>().updateMany(
            Filters.eq("parameterTypeMap", null),
            listOf(
                Updates.set(
                    "parameterTypeMap",
                    Document(mapOf(
                        "\$objectToArray" to "\$parameterTypes"
                    ))
                ),
                Updates.set(
                    "parameterTypeMap",
                    Document(mapOf(
                        "\$map" to mapOf(
                            "input" to "\$parameterTypeMap",
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
                    "parameterTypeMap",
                    Document(mapOf(
                        "\$arrayToObject" to "\$parameterTypeMap"
                    ))
                )
            )
        )
    }
}