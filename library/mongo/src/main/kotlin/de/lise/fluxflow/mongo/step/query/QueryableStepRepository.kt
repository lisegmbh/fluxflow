package de.lise.fluxflow.mongo.step.query

import de.lise.fluxflow.mongo.query.QueryableRepository
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.query.filter.StepDocumentFilter

interface QueryableStepRepository : QueryableRepository<StepDocument, StepDocumentFilter>