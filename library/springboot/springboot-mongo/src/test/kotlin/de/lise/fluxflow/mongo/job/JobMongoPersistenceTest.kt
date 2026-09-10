package de.lise.fluxflow.mongo.job

import com.mongodb.MongoClientSettings
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.persistence.job.JobData
import org.assertj.core.api.Assertions.assertThat
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.data.mongodb.core.MongoTemplate

@ExtendWith(OutputCaptureExtension::class)
class JobMongoPersistenceTest {
    @Test
    fun `enumerates scheduled references from projected BSON without materializing jobs`(output: CapturedOutput) {
        val jobRepository = mock<JobRepository>()
        val queryableRepository = mock<MongoFlowQueryRepository<JobDocument>>()
        val queryMapper = mock<QueryMapper<JobData, JobDocument>>()
        val mongoTemplate = mock<MongoTemplate>()
        val collection = mock<MongoCollection<Document>>()
        val documents = mock<FindIterable<Document>>()
        val cursor = mock<MongoCursor<Document>>()
        val objectId = ObjectId()

        whenever(mongoTemplate.getCollectionName(JobDocument::class.java)).thenReturn("jobs")
        whenever(mongoTemplate.getCollection("jobs")).thenReturn(collection)
        whenever(collection.find(any<Bson>())).thenReturn(documents)
        whenever(documents.projection(any<Bson>())).thenReturn(documents)
        whenever(documents.iterator()).thenReturn(cursor)
        whenever(cursor.hasNext()).thenReturn(true, true, true, true, true, false)
        whenever(cursor.next()).thenReturn(
            Document("_id", "good-before").append("workflowId", "workflow-1"),
            Document("_id", objectId)
                .append("workflowId", "workflow-1")
                .append("parameters", Document("_class", "untrusted.Value")),
            Document("_id", "missing-workflow-id"),
            Document("_id", 42).append("workflowId", "workflow-1"),
            Document("_id", "good-after").append("workflowId", "workflow-1"),
        )

        val persistence = JobMongoPersistence(
            jobRepository,
            queryableRepository,
            queryMapper,
            mongoTemplate,
        )

        val references = persistence.findScheduledJobReferences()

        assertThat(references.map { it.jobIdentifier.value })
            .containsExactly("good-before", objectId.toHexString(), "good-after")
        assertThat(references.map { it.workflowIdentifier.value })
            .containsOnly("workflow-1")

        val filter = argumentCaptor<Bson>()
        verify(collection).find(filter.capture())
        assertThat(filter.firstValue.toDocument())
            .isEqualTo(BsonDocument.parse("{ 'jobStatus': 'Scheduled' }"))

        val projection = argumentCaptor<Bson>()
        verify(documents).projection(projection.capture())
        assertThat(projection.firstValue.toDocument())
            .isEqualTo(BsonDocument.parse("{ '_id': 1, 'workflowId': 1 }"))
        verifyNoInteractions(jobRepository, queryableRepository, queryMapper)
        assertThat(output)
            .contains("Skipping malformed scheduled job reference with identifier \"missing-workflow-id\".")
            .contains("Skipping malformed scheduled job reference with identifier \"<unavailable>\".")
    }

    private fun Bson.toDocument(): BsonDocument = toBsonDocument(
        Document::class.java,
        MongoClientSettings.getDefaultCodecRegistry(),
    )
}
