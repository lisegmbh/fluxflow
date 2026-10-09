package de.lise.fluxflow.mongo

import org.bson.conversions.Bson
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.projection.EntityProjection

/** Keeps type validation on the synchronous conversion path. */
internal class GuardedMongoConverter(
    private val delegate: MongoConverter,
    private val policy: MongoDocumentTypePolicy,
) : MongoConverter by delegate {
    override fun <S : Any> read(type: Class<S>, source: Bson): S {
        policy.validate(type, source)
        return delegate.read(type, source)
    }

    override fun <R : Any> project(projection: EntityProjection<R, *>, source: Bson): R {
        policy.validate(projection.domainType.type, source)
        return delegate.project(projection, source)
    }

    override fun write(source: Any, sink: Bson) {
        delegate.write(source, sink)
        policy.validate(source.javaClass, sink)
    }
}
