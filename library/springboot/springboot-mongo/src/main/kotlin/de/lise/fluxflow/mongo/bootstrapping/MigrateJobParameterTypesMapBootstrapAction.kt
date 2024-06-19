package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import de.lise.fluxflow.mongo.job.JobDocument
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate

private const val PARAMETER_TYPE_MAP = "parameterTypeMap"

class MigrateJobParameterTypesMapBootstrapAction(mongoTemplate: MongoTemplate) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        ensureCollection<JobDocument>().updateMany(
            Filters.and(
                Filters.or(
                    Filters.exists(PARAMETER_TYPE_MAP, false),
                    Filters.eq(PARAMETER_TYPE_MAP, null),
                ),
                Filters.exists("parameterEntries", false)
            ),
            listOf(
                Updates.set(
                    PARAMETER_TYPE_MAP,
                    Document(mapOf(
                        "\$objectToArray" to "\$parameterTypes"
                    ))
                ),
                Updates.set(
                    PARAMETER_TYPE_MAP,
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
                    PARAMETER_TYPE_MAP,
                    Document(mapOf(
                        "\$arrayToObject" to "\$parameterTypeMap"
                    ))
                )
            )
        )
    }
}