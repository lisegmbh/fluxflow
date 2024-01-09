package de.lise.fluxflow.api.workflow.query.sort

import de.lise.fluxflow.query.sort.Direction

data class WorkflowIdSort<TWorkflowModel>(override val direction: Direction) : WorkflowSort<TWorkflowModel>