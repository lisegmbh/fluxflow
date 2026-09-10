package de.lise.fluxflow.mongo.step

import de.lise.fluxflow.mongo.step.query.QueryableStepRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*

@NoRepositoryBean
interface StepRepository : MongoRepository<StepDocument, String>, QueryableStepRepository {
    fun findByWorkflowId(workflowId: String): List<StepDocument>
    fun findByWorkflowIdAndId(workflowId: String, id: String): Optional<StepDocument>
}
