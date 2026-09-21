package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeRole
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

open class FluxFlowTypeManifestExtension @Inject constructor(
    objects: ObjectFactory,
) {
    internal val declarations: ListProperty<String> = objects
        .listProperty(String::class.java)
        .convention(emptyList())

    fun step(key: String, binaryClassName: String) = register(TypeRole.STEP, key, binaryClassName)

    fun job(key: String, binaryClassName: String) = register(TypeRole.JOB, key, binaryClassName)

    fun model(key: String, binaryClassName: String) = register(TypeRole.MODEL, key, binaryClassName)

    fun value(key: String, binaryClassName: String) = register(TypeRole.VALUE, key, binaryClassName)

    private fun register(role: TypeRole, key: String, binaryClassName: String) {
        declarations.add(ManifestDeclarationCodec.encode(role, key, binaryClassName))
    }
}
