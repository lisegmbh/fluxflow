package de.lise.fluxflow.mongo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper

class MongoTypeKeyResolverTest {
    @Test
    fun `D08 resolves the type key actually configured on the host converter`() {
        assertThat(MongoTypeKeyResolver.resolve(DefaultMongoTypeMapper("@type")))
            .isEqualTo("@type")
        assertThat(MongoTypeKeyResolver.resolve(DefaultMongoTypeMapper()))
            .isEqualTo("_class")
    }

    @Test
    fun `D08 rejects a host converter with disabled type metadata`() {
        assertThatThrownBy {
            MongoTypeKeyResolver.resolve(DefaultMongoTypeMapper(null))
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("type metadata")
    }
}
