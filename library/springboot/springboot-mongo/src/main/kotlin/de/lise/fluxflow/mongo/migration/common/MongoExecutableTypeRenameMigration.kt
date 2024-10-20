package de.lise.fluxflow.mongo.migration.common

import de.lise.fluxflow.migration.ExecutableMigration
import de.lise.fluxflow.migration.common.TypeRenameMigration
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateMulti
import kotlin.reflect.KProperty1

class MongoExecutableTypeRenameMigration(
    override val migration: TypeRenameMigration,
    private val isKind: Boolean,
    private val mongoTemplate: MongoTemplate
) : ExecutableMigration {
    override fun execute() {
        if (!isKind) {
            updateType(WorkflowDocument::modelType)
            updateType<WorkflowDocument>("${WorkflowDocument::model.name}._class")

            updateTypeEntries<StepDocument>(StepDocument::metadataEntries)
            updateTypeEntries<StepDocument>(StepDocument::dataEntries)

            updateTypeEntries<JobDocument>(JobDocument::parameterEntries)
        }

        updateType(StepDocument::kind)
        updateType(JobDocument::kind)
    }

    private inline fun <reified TDocument : Any, TProp> updateType(
        prop: KProperty1<TDocument, TProp>
    ) {
        updateType<TDocument>(prop.name)
    }

    private inline fun <reified T : Any> updateTypeEntries(
        prop: KProperty1<T, TypedRecords<*>?>
    ) {
        updateTypeEntries<T>(prop.name)
    }

    private inline fun <reified T : Any> updateTypeEntries(
        key: String
    ) {
        mongoTemplate.updateMulti<T>(
            Query.query(
                Criteria.where("${key}.jvmTypes.entries.type")
                    .`is`(migration.originalName)
            ),
            Update()
                .set("${key}.jvmTypes.entries.$.type", migration.newName)
        )
    }

    private inline fun <reified T : Any> updateType(
        key: String,
    ) {
        mongoTemplate.updateMulti<T>(
            Query.query(
                Criteria
                    .where(key)
                    .isEqualTo(migration.originalName)
            ),
            Update()
                .set(key, migration.newName),
        )
    }
}