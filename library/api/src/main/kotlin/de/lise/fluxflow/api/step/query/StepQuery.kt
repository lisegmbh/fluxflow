package de.lise.fluxflow.api.step.query

import de.lise.fluxflow.api.step.query.filter.StepFilter
import de.lise.fluxflow.api.step.query.sort.StepSort
import de.lise.fluxflow.query.Query

typealias StepQuery = Query<StepFilter, StepSort>