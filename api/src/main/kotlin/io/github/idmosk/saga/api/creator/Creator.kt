package io.github.idmosk.saga.api.creator

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.queue.model.SagasResult
import io.github.idmosk.saga.queue.model.enums.SagasResultEnum
import io.github.idmosk.saga.storage.model.CreateModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.util.UUID
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider
import io.github.idmosk.saga.storage.provider.Provider as StorageProvider

/**
 * A class for creating saga
 */
class Creator(
    private val storageProvider: StorageProvider,
    private val queueProvider: QueueProvider,
) {
    suspend fun create(model: io.github.idmosk.saga.api.creator.NewSaga): io.github.idmosk.saga.api.creator.Creator.Manager {
        val m =
            storageProvider.create(
                CreateModel(
                    model.businessId,
                    model.stepsForward,
                    model.stepsBackward,
                    model.deadLine,
                    model.retries,
                    model.retriesTimeout,
                ),
            )

        return Manager(m.id)
    }

    /**
     * A class-helper for running and waiting for sagas result
     */
    inner class Manager(val id: UUID) {
        private var channel: Channel<SagasResult>? = null
        private var scope: CoroutineScope? = null
        private var job: Job? = null

        /**
         * A method for running saga
         */
        fun start(): Creator.Manager {
            queueProvider.putNextStep(NextStep(id))
            return this
        }

        /**
         * A method that must be called before [await]
         */
        fun listen(
            scope: CoroutineScope,
            businessId: String,
        ): Creator.Manager {
            if (channel != null && this.scope != null && job != null) {
                throw Exception("already registered")
            }
            this.scope = scope
            channel = Channel()
            job = queueProvider.pullSagasResults(this.scope!!, channel!!, businessId)
            return this
        }

        /**
         * A method that should be called if [listen] was called and [await] wasn't call to leak protection
         */
        fun unregister(): Creator.Manager {
            if (channel == null || scope == null || job == null) {
                throw Exception("not registered")
            }
            channel!!.close()
            job!!.cancel()
            channel = null
            job = null
            return this
        }

        /**
         * A method for waiting sagas result
         */
        suspend fun await(): Boolean {
            if (channel == null || scope == null || job == null) {
                throw Exception("not registered")
            }
            return withContext(Dispatchers.IO) {
                try {
                    channel!!.receive().sagasResult == SagasResultEnum.OK
                } finally {
                    channel!!.close()
                    job!!.cancel()
                }
            }
        }
    }
}
