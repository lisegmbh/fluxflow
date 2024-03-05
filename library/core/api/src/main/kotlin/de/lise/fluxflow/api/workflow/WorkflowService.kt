package de.lise.fluxflow.api.workflow

interface WorkflowService :
    WorkflowStarterService,
    WorkflowQueryService,
    WorkflowUpdateService,
    WorkflowRemovalService