package de.lise.fluxflow.api.workflow.flowquery

import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.workflow.Workflow

typealias WorkflowQuery<TWorkflowModel> = FlowQuery<Workflow<TWorkflowModel>, Workflow<TWorkflowModel>>