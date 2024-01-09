package de.lise.fluxflow.api.workflow.query

import de.lise.fluxflow.api.workflow.query.filter.WorkflowFilter
import de.lise.fluxflow.api.workflow.query.sort.WorkflowSort
import de.lise.fluxflow.query.Query

typealias WorkflowQuery<TWorkflowModel> = Query<WorkflowFilter<TWorkflowModel>, WorkflowSort<TWorkflowModel>>