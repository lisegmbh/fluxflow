package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.Api

enum class Status {
    @Api
    Active,
    @Api
    Canceled,
    @Api
    Completed
}