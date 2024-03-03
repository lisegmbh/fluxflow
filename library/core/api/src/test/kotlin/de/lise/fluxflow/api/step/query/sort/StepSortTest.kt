package de.lise.fluxflow.api.step.query.sort

import de.lise.fluxflow.query.sort.Direction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StepSortTest {
    @Test
    fun `id should create a sort by id with the provided direction`() {
        // Act
        val ascendingById = StepSort.id(Direction.Ascending)
        val descendingById = StepSort.id(Direction.Descending)

        // Assert
        assertThat(ascendingById.let { it as? StepIdSort }?.direction).isEqualTo(Direction.Ascending)
        assertThat(descendingById.let { it as? StepIdSort }?.direction).isEqualTo(Direction.Descending)
    }
}