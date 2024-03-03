package de.lise.fluxflow.test.scheduling

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class TestCancellationKey(
    val workflowIdentifier: WorkflowIdentifier,
    val key: CancellationKey
)

class TestSchedulingService(
    private val callbacks: MutableSet<SchedulingCallback>
) : SchedulingService {

    constructor(vararg callbacks: SchedulingCallback)
            : this(callbacks.toMutableSet())


    private val executorService = Executors.newCachedThreadPool()

    private val concurrentCancellationMap = ConcurrentHashMap<TestCancellationKey, Boolean>()

    override fun schedule(pointInTime: Instant, schedulingReference: SchedulingReference) {
        val remainingTimeInMillis = pointInTime.toEpochMilli() - Instant.now().toEpochMilli()
        if (remainingTimeInMillis <= 0) {
            notify(schedulingReference)
            return
        }
        schedulingReference.cancellationKey?.let {
            concurrentCancellationMap.put(
                TestCancellationKey(
                    schedulingReference.workflowIdentifier,
                    it
                ),
                false
            )
        }
        executorService.submit{
            Thread.sleep(remainingTimeInMillis)

            val isCanceled = schedulingReference.cancellationKey?.let {
                val key = TestCancellationKey(
                    schedulingReference.workflowIdentifier,
                    it
                )

                val result = concurrentCancellationMap.getOrDefault(key, false)
                concurrentCancellationMap.remove(key)
                result
            } ?: false

            if(!isCanceled) {
                notify(schedulingReference)
            }

        }
    }

    override fun cancel(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey) {
        concurrentCancellationMap[TestCancellationKey(
            workflowIdentifier,
            cancellationKey
        )] = true
    }

    override fun registerListener(callback: SchedulingCallback) {
        callbacks.add(callback)
    }

    private fun notify(schedulingReference: SchedulingReference) {
        callbacks.forEach {
            try {
                it.onScheduled(schedulingReference)
            } catch (e: Exception) {
                Logger.error(
                    "An error occurred invoking callback ({}) for scheduled event {}.",
                    it,
                    schedulingReference,
                    e
                )
            }
        }
    }
    
    private companion object {
        val Logger = LoggerFactory.getLogger(TestSchedulingService::class.java)!!
    }
}