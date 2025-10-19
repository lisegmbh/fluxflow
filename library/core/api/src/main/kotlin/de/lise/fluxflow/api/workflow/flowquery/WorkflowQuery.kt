package de.lise.fluxflow.api.workflow.flowquery

import de.fluxflow.flowquery.query.Query
import de.lise.fluxflow.api.workflow.Workflow

typealias WorkflowQuery<TWorkflowModel> = Query<Workflow<TWorkflowModel>, Workflow<TWorkflowModel>>