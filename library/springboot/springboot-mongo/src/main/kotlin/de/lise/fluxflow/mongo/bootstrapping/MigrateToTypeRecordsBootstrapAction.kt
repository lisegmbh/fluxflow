package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.model.Filters
import de.lise.fluxflow.migration.MigrationError
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.StepRepository
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter

class MigrateToTypeRecordsBootstrapAction(
    private val failureAction: PartialFailureAction,
    private val stepRepository: StepRepository,
    private val jobRepository: JobRepository,
    private val mongoConverter: MongoConverter,
    mongoTemplate: MongoTemplate,
) : MongoBootstrapAction(mongoTemplate) {

    private data class MigrationFailure(
        val documentType: String,
        val id: String,
        val workflow: String,
        val reason: Exception
    )

    override fun setup() {
        val migrationFailures = mutableListOf<MigrationFailure>()

        val migratedStepDocuments = migrateStepDocuments(migrationFailures)
        val migratedJobDocuments = migrateJobDocuments(migrationFailures)

        if(migrationFailures.isEmpty()) {
            // Only persist the migrated documents if there has been no failures
            stepRepository.saveAll(migratedStepDocuments)
            jobRepository.saveAll(migratedJobDocuments)
        } else {
            throw MigrationError(
                "Migration failed for one or more documents: ${
                    migrationFailures.joinToString(", ") { 
                        "${it.documentType}/${it.id}" 
                    }
                }"
            )
        }
    }


    private fun migrateStepDocuments(
        migrationFailures: MutableList<MigrationFailure>
    ): List<StepDocument> {
        val stepCollection = ensureCollection<StepDocument>()
        val stepsToMigrate = stepCollection.find(
            Filters.or(
                Filters.not(Filters.exists("dataEntries")),
                Filters.not(Filters.exists("metadataEntries"))
            ),
        ).toList()
            .mapNotNull { document ->
                try {
                    mongoConverter.read(StepDocument::class.java, document)
                } catch (e: Exception) {
                    handleTypeActivationException("workflow", document, e)?.let { failure ->
                        migrationFailures.add(failure)
                    }

                    null
                }
            }.map {
                val stepData = it.toStepData()
                it.copy(
                    dataEntries = TypedRecords.fromData(stepData.data),
                    metadataEntries = TypedRecords.fromData(stepData.metadata)
                )
            }

        return stepsToMigrate
    }


    private fun migrateJobDocuments(
        migrationFailures: MutableList<MigrationFailure>
    ): List<JobDocument> {
        val jobCollection = ensureCollection<JobDocument>()
        val jobsToMigrate = jobCollection.find(
            Filters.not(Filters.exists("parameterEntries"))
        ).toList().mapNotNull { document ->
            try {
                mongoConverter.read(JobDocument::class.java, document)
            } catch (e: Exception) {
                handleTypeActivationException("job", document, e)?.let { failure ->
                    migrationFailures.add(failure)
                }
                null
            }
        }.map {
            val jobData = it.toJobData()
            it.copy(
                parameterEntries = TypedRecords.fromData(jobData.parameters)
            )
        }

        return jobsToMigrate
    }


    private fun handleTypeActivationException(
        type: String,
        document: Document,
        reason: Exception
    ): MigrationFailure? {
        // We don't need to distinguish between job and step documents,
        // as the properties _id and workflowId are present on both document types.
        val id = document.getObjectId("_id").toHexString()
        val workflow = document.getString("workflowId")

        Logger.warn(
            "Failed to migrate {} document '{}' belonging " +
                    "to the workflow '{}', " +
                    "because an exception occurred during activation. See inner exception for more details.",
            type,
            id,
            workflow,
            reason
        )

        return when (failureAction) {
            PartialFailureAction.Fail -> MigrationFailure(
                type,
                id,
                workflow,
                reason
            )

            PartialFailureAction.Warn -> null
        }
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(MigrateToTypeRecordsBootstrapAction::class.java)!!
    }
}