package de.lise.fluxflow.springboot.types.fixtures.defaultapp

import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.step.Step

@Step("scanned-step")
class DefaultScannedStep

@Job("scanned-job")
class DefaultScannedJob

class ExplicitUnannotatedModel
