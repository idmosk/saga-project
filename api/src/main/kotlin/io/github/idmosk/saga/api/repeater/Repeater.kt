package io.github.idmosk.saga.api.repeater

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.storage.model.enums.Status
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.timerTask
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider
import io.github.idmosk.saga.storage.provider.Provider as StorageProvider

/**
 * A class for start retries
 * @property periodSeconds how often scan will be started
 * @property fetchSize how many items will be fetched at a time.
 */
class Repeater(
    private val storageProvider: StorageProvider,
    private val queueProvider: QueueProvider,
    private val periodSeconds: Long = 1,
    private val fetchSize: Int = 100,
) {
    private val timer = Timer()
    private lateinit var supervisor: CompletableJob

    /**
     * Method that starts a prepared [Repeater]
     */
    fun start(scope: CoroutineScope) {
        supervisor = SupervisorJob(scope.coroutineContext[Job])

        scope.launch {
            timer.schedule(
                timerTask {
                    launch(supervisor) { retry() }
                },
                0,
                periodSeconds * 1000,
            )

            println("repeater started")
        }
    }

    /**
     * The method that should be called to shut down [Repeater] gracefully
     */
    suspend fun stop() {
        timer.cancel()
        supervisor.complete()
        supervisor.join()
        println("repeater ended")
    }

    private suspend fun retry() {
        var fromCreatedAt = LocalDateTime.MIN
        var fromId: UUID? = null

        while (true) {
            val models = storageProvider.fetchForRetry(fromCreatedAt, fromId, fetchSize)
            if (models.isEmpty()) {
                break
            }
            models.forEach {
                if (it.step > 0) {
                    if (storageProvider.changeStatus(it.id, it.status, Status.FORWARD)) {
                        queueProvider.putNextStep(NextStep(it.id))
                    } else {
                        // status doesn't change (probably already have been changed)
                        println("already have been changed")
                    }
                } else {
                    if (storageProvider.changeStatus(it.id, it.status, Status.BACKWARD)) {
                        queueProvider.putNextStep(NextStep(it.id))
                    } else {
                        // status doesn't change (probably already have changed)
                        println("already have been changed")
                    }
                }
            }
            fromCreatedAt = models[models.size - 1].createdAt
            fromId = models[models.size - 1].id
        }
    }
}
