package de.lise.fluxflow.mongo

import org.springframework.data.mapping.callback.EntityCallbacks
import org.springframework.data.mongodb.core.convert.MappingMongoConverter

/**
 * Customizes the converter used only by FluxFlow's internal Mongo access.
 *
 * The configuration deliberately does not expose the converter itself. In particular, changing
 * its type mapper or mapping context would let an application bypass FluxFlow's type boundary.
 */
fun interface FluxFlowMongoConverterCustomizer {
    fun customize(configuration: FluxFlowMongoConverterCustomization)
}

/** Safe, explicit subset of [MappingMongoConverter] customization supported by FluxFlow. */
class FluxFlowMongoConverterCustomization internal constructor(
    private val converter: MappingMongoConverter,
) {
    /** Replaces dots in map keys before they are persisted; `null` restores Spring Data's rejection. */
    fun setMapKeyDotReplacement(replacement: String?) {
        converter.setMapKeyDotReplacement(replacement)
    }

    /** Keeps dotted map keys unchanged. This requires a MongoDB version that accepts such keys. */
    fun preserveMapKeys() {
        converter.preserveMapKeys(true)
    }

    /** Replaces the callbacks invoked by the internal converter, including `AfterConvertCallback`. */
    fun setEntityCallbacks(callbacks: EntityCallbacks) {
        converter.setEntityCallbacks(callbacks)
    }
}
