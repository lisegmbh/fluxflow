package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import de.lise.fluxflow.test.scheduling.util.BusyWait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest(classes = [TestingConfiguration::class])
class JobIT {
    @Autowired
    var workflowStarterService: WorkflowStarterService? = null
    @Autowired
    var jobService: JobService? = null

    companion object {
        var jobHasRun: Boolean = false
        var jobParameterValue: String? = null
        var jobJobParameterValue: Job? = null
        var jobWorkflowParameterValue: Workflow<*>? = null
        var jobWorkflowStarterServiceParameterValue: WorkflowStarterService? = null
        var reschedulingRepetitions: Int = 0
    }

    @Test
    fun `scheduled jobs should get executed`() {
        // Act
        jobHasRun = false
        workflowStarterService!!.start(
            Any(),
            Continuation.job(Instant.now().plusSeconds(3), TestJob())
        )

        // Assert
        BusyWait.toBeTrue(5) {
            jobHasRun
        }
    }

    @Test
    fun `scheduled jobs should remain in scheduled state as long as they haven't run`() {
        // Act
        jobHasRun = false
        val workflow = workflowStarterService!!.start(
            Any(),
            Continuation.job(Instant.now().plusSeconds(1), TestJob())
        )

        // Act
        val jobBeforeExecution = jobService!!.findAllJobs(workflow).single()

        // Assert
        assertThat(jobBeforeExecution.status).isEqualTo(JobStatus.Scheduled)
        BusyWait.toBeTrue(2) {
            jobHasRun
        }
    }

    @Test
    fun `scheduled jobs should be in executed state once they have been executed`() {
        // Act
        jobHasRun = false
        val workflow = workflowStarterService!!.start(
            Any(),
            Continuation.job(Instant.now().plusSeconds(1), TestJob())
        )

        // Act
        BusyWait.toBeTrue(2) {
            jobHasRun
        }
        val jobAfterExecution = jobService!!.findAllJobs(workflow).single()

        // Assert
        assertThat(jobAfterExecution.status).isEqualTo(JobStatus.Executed)
    }

    @Test
    fun `jobs should be configurable by providing constructor parameters`() {
        // Arrange
        val valuePassedToTheJob = "Hello World"
        
        // Act
        workflowStarterService!!.start(
            Any(),
            Continuation.job(
                Instant.now().plusSeconds(1),
                TestJobWithParam(valuePassedToTheJob)
            )
        )
        
        // Assert
        val valueSetByJob = BusyWait.toBeNonNull(3) {
            jobParameterValue
        }
        assertThat(valueSetByJob).isEqualTo(valuePassedToTheJob)
    }

    @Test
    fun `job payload functions should be able to receive the current workflow, job and values from the ioc container`() {
        // Act
        val workflow = workflowStarterService!!.start(
            Any(),
            Continuation.job(
                Instant.now().plusSeconds(1),
                TestJobWithFunctionParam()
            )
        )

        // Assert
        val valueSetByJob = BusyWait.toBeNonNull(3) {
            jobJobParameterValue
        }
        assertThat(valueSetByJob.workflow.identifier).isEqualTo(workflow.identifier)

        val workflowSetByJob = BusyWait.toBeNonNull(1) {
            jobWorkflowParameterValue
        }
        assertThat(workflowSetByJob.identifier).isEqualTo(workflow.identifier)

        val workflowServiceSetByJob = BusyWait.toBeNonNull(1) {
            jobWorkflowStarterServiceParameterValue
        }
        assertThat(workflowServiceSetByJob).isEqualTo(workflowStarterService)
    }

    @Test
    fun `job continuations returned by a job's payload function should be automatically scheduled`() {
        // Act
        workflowStarterService!!.start(
            Any(),
            Continuation.job(
                Instant.now().plusSeconds(1),
                TestReschedulingJob()
            )
        )

        // Assert
        val valueSetByJob = BusyWait(
            { reschedulingRepetitions },
            { it == -1 }
        ).waitForSeconds(3)
        assertThat(valueSetByJob).isEqualTo(-1)
    }
}

class TestJob{
    fun execute() {
        JobIT.jobHasRun = true
    }
}
class TestJobWithParam(
    val testOutput: String
) {
    fun execute() {
        JobIT.jobParameterValue = testOutput
    }
}
class TestJobWithFunctionParam {
    fun execute(
        job: Job,
        workflow: Workflow<*>,
        workflowStarterService: WorkflowStarterService
    ) {
        JobIT.jobJobParameterValue = job
        JobIT.jobWorkflowParameterValue = workflow
        JobIT.jobWorkflowStarterServiceParameterValue = workflowStarterService
    }
}

class TestReschedulingJob {
    fun execute(): Continuation<*> {
        JobIT.reschedulingRepetitions++
        return if(JobIT.reschedulingRepetitions >= 2) {
            JobIT.reschedulingRepetitions = -1
            Continuation.none()
        } else {
            Continuation.job(
                Instant.now().plusSeconds(1),
                TestReschedulingJob()
            )
        }
    }
}
