package de.lise.fluxflow.springboot.types.fixtures.explicitapp

import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultScannedStep
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(
    scanBasePackages = [
        "de.lise.fluxflow.springboot.types.fixtures.defaultapp",
        "de.lise.fluxflow.springboot.types.fixtures.defaultapp.nested",
    ],
    scanBasePackageClasses = [DefaultScannedStep::class],
)
open class ExplicitTypeApplication
