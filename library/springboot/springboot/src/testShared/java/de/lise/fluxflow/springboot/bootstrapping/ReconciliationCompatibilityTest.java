package de.lise.fluxflow.springboot.bootstrapping;

import de.lise.fluxflow.api.bootstrapping.BootstrapAction;
import de.lise.fluxflow.api.job.Job;
import de.lise.fluxflow.api.job.JobIdentifier;
import de.lise.fluxflow.api.job.JobService;
import de.lise.fluxflow.api.workflow.Workflow;
import de.lise.fluxflow.api.workflow.WorkflowIdentifier;
import de.lise.fluxflow.api.workflow.WorkflowService;
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence;
import de.lise.fluxflow.query.pagination.Page;
import de.lise.fluxflow.scheduling.SchedulingReference;
import de.lise.fluxflow.scheduling.SchedulingService;
import de.lise.fluxflow.springboot.configuration.BasicConfiguration;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReconciliationCompatibilityTest {
    @Test
    @SuppressWarnings({"unchecked", "deprecation"})
    void existingJavaConstructorAndOverridableFactoryScheduleReturnedJobs() {
        JobService jobs = mock(JobService.class);
        SchedulingService scheduling = mock(SchedulingService.class);
        Workflow<?> workflow = mock(Workflow.class);
        Job job = mock(Job.class);
        WorkflowIdentifier workflowId = new WorkflowIdentifier("legacy-workflow");
        JobIdentifier jobId = new JobIdentifier("legacy-job");
        Instant time = Instant.parse("2026-10-05T12:00:00Z");
        when(workflow.getIdentifier()).thenReturn(workflowId);
        doReturn(workflow).when(job).getWorkflow();
        when(job.getIdentifier()).thenReturn(jobId);
        when(job.getScheduledTime()).thenReturn(time);
        doReturn(Page.Companion.unpaged(List.of(job))).when(jobs).findAll(any(Function1.class));

        new ReconcileScheduledJobsBootstrapAction(jobs, scheduling).setup();
        new LegacyConfiguration().startupJobReconciliation(jobs, scheduling).setup();

        verify(scheduling, times(2)).schedule(eq(time),
            eq(new SchedulingReference(workflowId, jobId, null, null)));
        verify(jobs, never()).getJob(any(), any());
    }

    @Test
    @SuppressWarnings({"unchecked", "deprecation"})
    void legacyBulkTypeErrorsRemainFailFast() {
        JobService jobs = mock(JobService.class);
        SchedulingService scheduling = mock(SchedulingService.class);
        RuntimeException rejected = new de.lise.fluxflow.reflection.types.UnknownTypeException(
            de.lise.fluxflow.reflection.types.TypeRole.VALUE, "rejected-value");
        doThrow(rejected).when(jobs).findAll(any(Function1.class));
        assertThatThrownBy(() -> new ReconcileScheduledJobsBootstrapAction(jobs, scheduling).setup())
            .isSameAs(rejected);
        assertThatThrownBy(() -> new BasicConfiguration().startupJobReconciliation(jobs, scheduling).setup())
            .isSameAs(rejected);
        verifyNoInteractions(scheduling);
    }

    @Test
    void productionBeanUsesRawReferencesWithoutBulkReads() {
        JobService jobs = mock(JobService.class);
        SchedulingService scheduling = mock(SchedulingService.class);
        ScheduledJobReferencePersistence references = mock(ScheduledJobReferencePersistence.class);
        when(references.findScheduledJobReferences()).thenReturn(List.of());
        try (AnnotationConfigApplicationContext context = context(jobs, scheduling)) {
            context.registerBean("testReferences", ScheduledJobReferencePersistence.class, () -> references,
                definition -> definition.setPrimary(true));
            context.registerBean("testWorkflows", WorkflowService.class, () -> mock(WorkflowService.class),
                definition -> definition.setPrimary(true));
            context.refresh();
            context.getBean("startupJobReconciliation", BootstrapAction.class).setup();
            verify(references, times(2)).findScheduledJobReferences(); // refresh bootstrap and explicit setup
            verifyNoInteractions(jobs, scheduling);
        }
    }

    @Test
    void missingWorkflowServiceCannotSelectLegacyFactoryInsteadOfProductionBean() {
        try (AnnotationConfigApplicationContext context = context(mock(JobService.class), mock(SchedulingService.class))) {
            context.addBeanFactoryPostProcessor(factory -> {
                if (factory.containsBeanDefinition("workflowService")) {
                    ((org.springframework.beans.factory.support.BeanDefinitionRegistry) factory)
                        .removeBeanDefinition("workflowService");
                }
            });
            assertThatThrownBy(() -> {
                context.refresh();
                context.getBean("startupJobReconciliation", BootstrapAction.class);
            })
                .hasRootCauseInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException.class);
        }
    }

    private AnnotationConfigApplicationContext context(JobService jobs, SchedulingService scheduling) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("reconciliation",
            Map.of("fluxflow.scheduling.reconcileOnStartup", "true")));
        context.register(BasicConfiguration.class);
        context.registerBean("testJobs", JobService.class, () -> jobs, definition -> definition.setPrimary(true));
        context.registerBean("testScheduling", SchedulingService.class, () -> scheduling,
            definition -> definition.setPrimary(true));
        context.addBeanFactoryPostProcessor(factory -> {
            for (String name : factory.getBeanDefinitionNames()) factory.getBeanDefinition(name).setLazyInit(true);
        });
        return context;
    }

    private static class LegacyConfiguration extends BasicConfiguration {
        @Override
        public BootstrapAction startupJobReconciliation(JobService jobs, SchedulingService scheduling) {
            return super.startupJobReconciliation(jobs, scheduling);
        }
    }
}
