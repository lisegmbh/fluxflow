package de.lise.fluxflow.mongo.security.production;

import de.lise.fluxflow.mongo.generic.SimpleType;
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException;
import de.lise.fluxflow.mongo.generic.ValueTypeConverter;
import de.lise.fluxflow.reflection.types.TypeRegistry;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueTypeConverterJavaCompatibilityTest {
    @Test void originalThreeArgumentConstructorRemainsUsableWithoutHostConversions() {
        TypeRegistry registry = TypeRegistry.Companion.create(getClass().getClassLoader(), List.of());
        ValueTypeConverter converter = new ValueTypeConverter(registry, 100, 100_000);
        assertThat(converter.assertType(new SimpleType("java.lang.String"), "kept")).isEqualTo("kept");
        assertThatThrownBy(() -> converter.assertType(new SimpleType("java.time.LocalDate"), new Date(0)))
            .isInstanceOf(ValueTypeConversionException.class);
    }
}