package de.lise.fluxflow.springboot.rest.patch

class PatchRegistry<TElement>(
    private val registrations: List<PatchCapability<TElement>>
) {
    constructor(
        vararg capabilities: PatchCapability<TElement>
    ) : this(
        capabilities.toList()
    )

    fun toOperation(
        original: TElement,
        patch: JsonPatch
    ): PatchAction<TElement> {
        val operations = patch.operations.associateWith {
            findForOperation(
                original,
                it
            )
        }
        val missingOperations = operations.filter {
            it.value == null
        }.map { it.key }

        if (missingOperations.isNotEmpty()) {
            throw InvalidPatchException(
                """
                The request contained unsupported patch operations:
                ${missingOperations.toEnumerationString()}
                
                Only the following operations are supported:
                ${
                    registrations.map {
                        it.description
                    }.toEnumerationString()
                }
            """.trimIndent()
            )
        }

        val all = operations.mapNotNull { it.value }

        return PatchAction { originalElement ->
            var element = originalElement
            for (singleOperation in all) {
                element = singleOperation.perform(element)
            }
            element
        }
    }

    private fun findForOperation(
        original: TElement,
        operation: JsonPatchOperation
    ): PatchAction<TElement>? {
        return registrations.firstNotNullOfOrNull {
            it.prepare(original, operation)
        }
    }

    private fun <T> Collection<T>.toEnumerationString(): String {
        return this.joinToString("\n") { element ->
            "-" + "$element".prependIndent("  ").substring(1)
        }
    }
}