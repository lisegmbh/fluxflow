package de.lise.fluxflow.test.persistence

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator

internal class AnyPolymorphicTypeValidator : PolymorphicTypeValidator() {
    override fun validateBaseType(config: MapperConfig<*>?, baseType: JavaType?): Validity {
        return Validity.ALLOWED
    }

    override fun validateSubClassName(config: MapperConfig<*>?, baseType: JavaType?, subClassName: String?): Validity {
        return Validity.ALLOWED
    }

    override fun validateSubType(config: MapperConfig<*>?, baseType: JavaType?, subType: JavaType?): Validity {
        return Validity.ALLOWED
    }

}