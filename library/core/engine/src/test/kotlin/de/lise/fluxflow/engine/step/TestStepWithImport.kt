package de.lise.fluxflow.engine.step

import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.step.Step

@Step
class TestStepWithImport(
    @Import
    val importedInformation: TestDataToBeImported 
)