package de.lise.fluxflow.api.job.query

import de.lise.fluxflow.api.job.query.filter.JobFilter
import de.lise.fluxflow.api.job.query.sort.JobSort
import de.lise.fluxflow.query.Query

typealias JobQuery = Query<JobFilter, JobSort>
