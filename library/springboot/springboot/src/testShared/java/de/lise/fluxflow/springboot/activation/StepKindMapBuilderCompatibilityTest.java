package de.lise.fluxflow.springboot.activation;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StepKindMapBuilderCompatibilityTest {
    @Test
    void keepsTheExistingTwoArgumentJavaConstructor() {
        StepKindMapBuilder builder = new StepKindMapBuilder(
            mock(ApplicationContext.class),
            getClass().getClassLoader()
        );

        assertThat(builder).isNotNull();
    }
}
