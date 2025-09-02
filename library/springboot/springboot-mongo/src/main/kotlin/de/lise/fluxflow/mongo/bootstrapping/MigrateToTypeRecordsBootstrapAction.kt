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

        val stepBuffer = ActionBuffer<StepDocument>(100) { migratedDocuments ->
            stepRepository.saveAll(migratedDocuments)
        }
        migrateStepDocuments(stepBuffer, migrationFailures)
        stepBuffer.flush()

        val jobBuffer = ActionBuffer<JobDocument>(100) { migratedDocuments ->
            jobRepository.saveAll(migratedDocuments)

        }
        migrateJobDocuments(jobBuffer, migrationFailures)
        jobBuffer.flush()

        if(migrationFailures.isNotEmpty()) {
            throw MigrationError(
                "Type record migration failed for one or more documents (see https://docs.fluxflow.cloud/see/1): ${
                    migrationFailures.joinToString(", ") { 
                        "${it.documentType}/${it.id}" 
                    }
                }"
            )
        }
    }


    private fun migrateStepDocuments(
        buffer: ActionBuffer<StepDocument>,
        migrationFailures: MutableList<MigrationFailure>
    ) {
        val stepCollection = ensureCollection<StepDocument>()
        val cursor = stepCollection.find(
            Filters.or(
                Filters.not(Filters.exists("dataEntries")),
                Filters.not(Filters.exists("metadataEntries"))
            ),
        ).cursor()

        while (cursor.hasNext()) {
            val doc = cursor.next()
            val stepDocument = try {
                mongoConverter.read(StepDocument::class.java, doc)
            } catch (e: Exception) {
                handleTypeActivationException("workflow", doc, e)?.let { failure ->
                    migrationFailures.add(failure)
                }
                null
            }
            stepDocument?.let {
                val stepData = it.toStepData()
                it.copy(
                    dataEntries = TypedRecords.fromData(stepData.data),
                    metadataEntries = TypedRecords.fromData(stepData.metadata)
                )
            }?.let {
                buffer.push(it)
            }
        }
    }

    private fun migrateJobDocuments(
        buffer: ActionBuffer<JobDocument>,
        migrationFailures: MutableList<MigrationFailure>
    ) {
        val jobCollection = ensureCollection<JobDocument>()

        val cursor = jobCollection.find(
            Filters.not(Filters.exists("parameterEntries"))
        ).cursor()

        while(cursor.hasNext()) {
            val doc = cursor.next()
            val jobDocument = try {
                mongoConverter.read(JobDocument::class.java, doc)
            }catch (e: Exception) {
                handleTypeActivationException("job", doc, e)?.let { failure ->
                    migrationFailures.add(failure)
                }
                null
            }
            jobDocument?.let {
                val jobData = it.toJobData()
                it.copy(
                    parameterEntries = TypedRecords.fromData(jobData.parameters)
                )
            }?.let {
                buffer.push(it)
            }
        }
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