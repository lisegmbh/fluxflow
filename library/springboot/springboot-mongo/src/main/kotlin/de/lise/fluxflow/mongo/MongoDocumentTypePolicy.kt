package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.Document
import org.bson.conversions.Bson
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

/** Validates every persisted type discriminator before Spring Data may resolve it. */
internal class MongoDocumentTypePolicy(
    private val aliases: FluxFlowMongoTypeAliases,
    private val typeKey: String = "_class",
    private val maxDepth: Int = 100,
    private val maxNodes: Int = 100_000,
) {
    init {
        require(maxDepth > 0) { "Mongo document maximum depth must be positive" }
        require(maxNodes > 0) { "Mongo document maximum node count must be positive" }
    }

    fun validate(targetType: Class<*>, source: Bson) {
        val root = source as? Document
            ?: throw IllegalArgumentException("Mongo data must be represented by a BSON Document")

        validateRootAlias(targetType, root)

        val rootRole = when (targetType) {
            StepDocument::class.java,
            JobDocument::class.java,
            StepDefinitionDocument::class.java -> TypeRole.VALUE
            else -> null
        }
        val pending = ArrayDeque<Frame>()
        root.entries.asSequence()
            .filterNot { (key, _) -> key == typeKey }
            .forEach { (key, value) ->
                if (targetType == WorkflowDocument::class.java && key == WorkflowDocument::model.name) {
                    pending.add(Frame(value, key, 1, TypeRole.MODEL, TypeRole.VALUE))
                } else {
                    pending.add(Frame(value, key, 1, rootRole, rootRole))
                }
            }

        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        var nodes = 1
        while (pending.isNotEmpty()) {
            val frame = pending.removeLast()
            nodes++
            require(nodes <= maxNodes) {
                "Mongo document exceeds the maximum node count of $maxNodes"
            }
            require(frame.depth <= maxDepth) {
                "Mongo document exceeds the maximum depth of $maxDepth at '${frame.path}'"
            }

            when (val value = frame.value) {
                is Map<*, *> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    validateAlias(value, frame.role, frame.path)
                    value.entries.asSequence()
                        .filterNot { (key, _) -> key == typeKey }
                        .forEach { (key, nested) ->
                            pending.add(
                                Frame(
                                    nested,
                                    "${frame.path}.$key",
                                    frame.depth + 1,
                                    frame.descendantRole,
                                    frame.descendantRole,
                                )
                            )
                        }
                }

                is Iterable<*> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    var index = 0
                    for (nested in value) {
                        require(nodes + pending.size < maxNodes) {
                            "Mongo document exceeds the maximum node count of $maxNodes"
                        }
                        pending.add(
                            Frame(
                                nested,
                                "${frame.path}[$index]",
                                frame.depth + 1,
                                frame.descendantRole,
                                frame.descendantRole,
                            )
                        )
                        index++
                    }
                }

                is Array<*> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    value.forEachIndexed { index, nested ->
                        pending.add(
                            Frame(
                                nested,
                                "${frame.path}[$index]",
                                frame.depth + 1,
                                frame.descendantRole,
                                frame.descendantRole,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun validateRootAlias(targetType: Class<*>, root: Document) {
        if (!root.containsKey(typeKey)) {
            return
        }
        val resolved = aliases.resolve(root[typeKey])
        if (targetType in aliases.rootTypes) {
            require(resolved == targetType) {
                "Mongo root type '${resolved.name}' does not match requested type '${targetType.name}'"
            }
        }
    }

    private fun validateAlias(document: Map<*, *>, role: TypeRole?, path: String) {
        if (!document.containsKey(typeKey)) {
            return
        }
        val alias = document[typeKey]
        try {
            if (aliases.isInfrastructure(alias)) {
                return
            }
            if (role == null) {
                aliases.resolve(alias)
            } else {
                aliases.resolve(role, alias)
            }
        } catch (exception: IllegalArgumentException) {
            exception.addSuppressed(IllegalArgumentException("Invalid Mongo type at '$path.$typeKey'"))
            throw exception
        }
    }

    private data class Frame(
        val value: Any?,
        val path: String,
        val depth: Int,
        val role: TypeRole?,
        val descendantRole: TypeRole?,
    )
}
