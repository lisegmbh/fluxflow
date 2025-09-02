package de.lise.fluxflow.mongo.job.query

import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobDocumentFilter
import de.lise.fluxflow.mongo.query.QueryableRepository

interface QueryableJobRepository : QueryableRepository<JobDocument, JobDocumentFilter>
