package de.lise.fluxflow.stereotyped

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.reflection.activation.parameter.*

interface DefaultResolvers {
    companion object {
        fun forWorkflow(workflow: ParameterProvider<Workflow<*>>): Array<ParameterResolver> {
            return arrayOf(
                CallbackParameterResolver(
                    ParamMatcher.isAssignableFrom(Workflow::class),
                    workflow
                ),
                CallbackParameterResolver(
                    ParamMatcher.isAssignableFrom(WorkflowIdentifier::class),
                    workflow.map { it.id }
                )
            )
        }
        
        fun forStep(step: ParameterProvider<Step>): Array<ParameterResolver> {
            return arrayOf(
                CallbackParameterResolver(
                    ParamMatcher.isAssignableFrom(Step::class),
                    step
                ),
                CallbackParameterResolver(
                    ParamMatcher.isAssignableFrom(StepIdentifier::class),
                    step.map { it.identifier }
                ),
                *forWorkflow(step.map { it.workflow })
            )
        }
        
        fun forJob(job: ParameterProvider<Job>): Array<ParameterResolver> {
            return arrayOf(
                CallbackParameterResolver(
                    ParamMatcher.isAssignableFrom(Job::class),
                    job
                ),
                CallbackParameterResolver(
                    ParamMatcher.isAssignableFrom(JobIdentifier::class),
                    job.map { it.identifier }
                ),
                *forWorkflow(job.map { it.workflow })
            )
        }
    }
}