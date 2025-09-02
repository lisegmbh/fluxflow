package de.lise.fluxflow.mongo.bootstrapping.collation

import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.data.mongodb.core.query.Collation

@ConfigurationProperties(prefix = "fluxflow.mongo.collation")
open class CollationConfiguration @ConstructorBinding constructor(
    private val default: CollationOptions?,
    private val workflow: CollationOptions?,
    private val step: CollationOptions?,
    private val job: CollationOptions?,
    private val continuationRecord: CollationOptions?
): CollationConfigurer {
    override fun <TDocument : Any> configureCollation(type: Class<TDocument>): Collation? {
        return if(WorkflowDocument::class.java.isAssignableFrom(type)) {
             createCollation(workflow)
        } else if(StepDocument::class.java.isAssignableFrom(type)) {
            createCollation(step)
        } else if(JobDocument::class.java.isAssignableFrom(type)) {
            createCollation(job)
        } else if(ContinuationRecordDocument::class.java.isAssignableFrom(type)) {
            createCollation(continuationRecord)
        } else {
            return createCollation(null)
        }
    }

    private fun createCollation(collectionSpecificOptions: CollationOptions?): Collation? {
        return (collectionSpecificOptions ?: default)?.build()
    }
}