package io.github.idmosk.saga.api.router

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.queue.model.SagasResult
import io.github.idmosk.saga.queue.model.Task
import io.github.idmosk.saga.queue.model.TasksResult
import io.github.idmosk.saga.queue.model.enums.Direction
import io.github.idmosk.saga.queue.model.enums.SagasResultEnum
import io.github.idmosk.saga.queue.model.enums.TasksResultEnum
import io.github.idmosk.saga.storage.model.GetModel
import io.github.idmosk.saga.storage.model.UpdateModel
import io.github.idmosk.saga.storage.model.enums.Status
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider
import io.github.idmosk.saga.storage.provider.Provider as StorageProvider

/**
 * A class for manage, update sagas status and route sagas steps
 * @property concurrency how many coroutines will be started
 */
class Router(
    private val storageProvider: StorageProvider,
    private val queueProvider: QueueProvider,
    private val concurrency: Int,
) {
    private lateinit var parentScope: CoroutineScope

    private lateinit var producersSupervisor: CompletableJob
    private val receiversSupervisor: CompletableJob = SupervisorJob()
    private val handlersSupervisor: CompletableJob = SupervisorJob()

    private lateinit var nextStepsChannel: Channel<NextStep>
    private lateinit var tasksResultsChannel: Channel<TasksResult>

    private lateinit var nextStepsProducerJob: Job
    private lateinit var tasksResultsProducerJob: Job

    private lateinit var nextStepsReceiverJob: Job

    private val nextStepsUndeliveredElementHandler = fun (e: NextStep) {
        queueProvider.putNextStep(e)
    }
    private val tasksResultsUndeliveredElementHandler = fun (e: TasksResult) {
        queueProvider.putTasksResult(e)
    }

    private val nextStepsProducerExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            run {
                e.printStackTrace()
                nextStepsProducerExceptionHandler()
            }
        }
    private val nextStepsReceiverExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            run {
                e.printStackTrace()
                nextStepsReceiverExceptionHandler()
            }
        }
    private val nextStepsHandleExceptionHandler =
        CoroutineExceptionHandler { _, t ->
            t.printStackTrace(); // TODO
        }
    private val tasksResultsProducerExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            run {
                e.printStackTrace()
                tasksResultsProducerExceptionHandler()
            }
        }
    private val tasksResultsReceiverExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            run {
                e.printStackTrace()
                tasksResultsReceiverExceptionHandler()
            }
        }
    private val tasksResultsHandleExceptionHandler =
        CoroutineExceptionHandler { _, t ->
            t.printStackTrace(); // TODO
        }

    private fun nextStepsProducerExceptionHandler() {
        parentScope.launch { launchNextSteps() }
    }

    private fun nextStepsReceiverExceptionHandler() {
        parentScope.launch { launchNextStepsReceiver() }
    }

    private fun tasksResultsProducerExceptionHandler() {
        parentScope.launch { launchTasksResults() }
    }

    private fun tasksResultsReceiverExceptionHandler() {
        parentScope.launch { launchTasksResultsReceiver() }
    }

    /**
     * Method that starts a prepared [Router]
     */
    fun start(scope: CoroutineScope) {
        parentScope = scope
        producersSupervisor = SupervisorJob(parentScope.coroutineContext[Job])

        parentScope.launch {
            launchNextSteps()
            launchTasksResults()

            println("router started")
            join()
            println("router ended")
        }
    }

    /**
     * The method that should be called to shut down [Router] gracefully
     */
    suspend fun stop() {
        nextStepsChannel.close()
        tasksResultsChannel.close()

        nextStepsProducerJob.cancel()
        tasksResultsProducerJob.cancel()

        producersSupervisor.complete()
        receiversSupervisor.complete()
        handlersSupervisor.complete()

        join()
    }

    private fun CoroutineScope.launchNextSteps() =
        launch {
            nextStepsChannel = Channel(onUndeliveredElement = nextStepsUndeliveredElementHandler)

            launchNextStepsProducer()
            repeat(concurrency) { nextStepsReceiverJob = launchNextStepsReceiver() }
        }

    private fun CoroutineScope.launchNextStepsProducer() =
        launch(coroutineContext + producersSupervisor + nextStepsProducerExceptionHandler) {
            nextStepsProducerJob = queueProvider.pullNextSteps(this, nextStepsChannel)
        }

    private fun CoroutineScope.launchNextStepsReceiver() =
        launch(coroutineContext + receiversSupervisor + nextStepsReceiverExceptionHandler) {
            for (it in nextStepsChannel) {
                handleNextStep(it)
            }
        }

    private suspend fun handleNextStep(nextStep: NextStep) =
        withContext(coroutineContext + handlersSupervisor + nextStepsHandleExceptionHandler) {
            planTask(
                makeStep(
                    storageProvider.get(nextStep.id),
                ),
            )
        }

    private fun CoroutineScope.launchTasksResults() =
        launch {
            tasksResultsChannel = Channel(onUndeliveredElement = tasksResultsUndeliveredElementHandler)

            launchTasksResultsProducer()
            repeat(concurrency) { launchTasksResultsReceiver() }
        }

    private fun CoroutineScope.launchTasksResultsProducer() =
        launch(coroutineContext + producersSupervisor + tasksResultsProducerExceptionHandler) {
            tasksResultsProducerJob = queueProvider.pullTasksResults(this, tasksResultsChannel)
        }

    private fun CoroutineScope.launchTasksResultsReceiver() =
        launch(coroutineContext + receiversSupervisor + tasksResultsReceiverExceptionHandler) {
            for (it in tasksResultsChannel) {
                handleTaskResult(it)
            }
        }

    private suspend fun handleTaskResult(tasksResult: TasksResult) =
        withContext(coroutineContext + handlersSupervisor + tasksResultsHandleExceptionHandler) {
            val model =
                when (tasksResult.result) {
                    TasksResultEnum.FORWARD_OK -> {
                        forwardOk(storageProvider.get(tasksResult.id))
                    }
                    TasksResultEnum.FORWARD_NOK -> {
                        forwardNok(storageProvider.get(tasksResult.id))
                    }
                    TasksResultEnum.BACKWARD_OK -> {
                        backwardOk(storageProvider.get(tasksResult.id))
                    }
                    TasksResultEnum.FORWARD_EXCEPTION -> {
                        forwardException(storageProvider.get(tasksResult.id))
                    }
                    TasksResultEnum.BACKWARD_EXCEPTION -> {
                        backwardException(storageProvider.get(tasksResult.id))
                    }
                    TasksResultEnum.FATAL -> {
                        fatal(storageProvider.get(tasksResult.id), tasksResult.message)
                    }
                }

            planTask(
                makeStep(model),
            )
        }

    private suspend fun makeStep(model: GetModel): GetModel {
        return when (model.deadLine?.isAfter(LocalDateTime.now())) {
            true, null -> {
                // not expired
                when (model.status) {
                    Status.NEW -> moveForward(model) // do first step
                    else -> {
                        return model
                    }
                }
            }
            false -> {
                // expired
                when (model.status) {
                    Status.NEW -> markAsExpired(model) // haven’t completed a single step yet - transfer to EXPIRED
                    Status.FORWARD -> turnBack(model) // have already taken one or more steps - change direction
                    else -> {
                        return model
                    }
                }
            }
        }
    }

    private suspend fun planTask(model: GetModel) {
        when (model.status) {
            Status.FORWARD -> {
                val step = model.stepsForward.elementAtOrElse(model.step - 1) { throw Exception("couldn't find forward step") }
                queueProvider.putTask(step, Task(model.id, model.businessId, Direction.FORWARD, step))
            }
            Status.BACKWARD -> {
                val step = model.stepsBackward.elementAtOrElse(abs(model.step) - 1) { throw Exception("couldn't find backward step") }
                if (step != null) {
                    queueProvider.putTask(step, Task(model.id, model.businessId, Direction.BACKWARD, step))
                } else {
                    // as if it was completed successfully and we are immediately planning the next step
                    planTask(
                        makeStep(
                            backwardOk(model),
                        ),
                    )
                }
            }
            Status.DONE -> {
                queueProvider.putSagasResult(model.businessId, SagasResult(model.id, model.businessId, SagasResultEnum.OK))
            }
            Status.EXPIRED, Status.ROLLED_BACK, Status.FAIL, Status.FATAL -> {
                queueProvider.putSagasResult(model.businessId, SagasResult(model.id, model.businessId, SagasResultEnum.NOK))
            }
            else -> {}
        }
    }

    private suspend fun moveForward(model: GetModel): GetModel {
        return storageProvider.update(
            UpdateModel.Builder(model)
                .setStatus(Status.FORWARD)
                .build(),
        )
    }

    private suspend fun turnBack(model: GetModel): GetModel {
        return storageProvider.update(
            UpdateModel.Builder(model)
                .setStep(model.step * -1 + 1)
                .setStatus(Status.BACKWARD)
                .setTriesMade(null)
                .build(),
        )
    }

    private suspend fun markAsExpired(model: GetModel): GetModel {
        return storageProvider.update(
            UpdateModel.Builder(model)
                .setStatus(Status.EXPIRED)
                .build(),
        )
    }

    private suspend fun forwardOk(model: GetModel): GetModel {
        val builder = UpdateModel.Builder(model)
        if (model.stepsForward.size == model.step) {
            builder.setStatus(Status.DONE)
        } else {
            builder
                .setStep(builder.step + 1)
                .setTriesMade(null)
        }
        return storageProvider.update(builder.build())
    }

    private suspend fun forwardNok(model: GetModel): GetModel {
        return storageProvider.update(
            UpdateModel.Builder(model)
                .setStep(model.step * -1 + 1)
                .setStatus(Status.BACKWARD)
                .build(),
        )
    }

    private suspend fun backwardOk(model: GetModel): GetModel {
        val builder = UpdateModel.Builder(model)
        builder.setStep(builder.step + 1)
        if (builder.step == 0) {
            builder.setStatus(Status.ROLLED_BACK)
        } else {
            builder.setTriesMade(null)
        }
        return storageProvider.update(builder.build())
    }

    private suspend fun forwardException(model: GetModel): GetModel {
        return if (model.retries == null || model.retriesTimeout == null) {
            turnBack(model)
        } else {
            if ((model.triesMade ?: 0) >= model.retries!!) {
                turnBack(model)
            } else {
                storageProvider.update(
                    UpdateModel.Builder(model)
                        .setTriesMade(model.triesMade?.plus(1) ?: 1)
                        .setStatus(Status.WAITING_RETRY)
                        .setNextRetries(LocalDateTime.now().plus(model.retriesTimeout))
                        .build(),
                )
            }
        }
    }

    private suspend fun backwardException(model: GetModel): GetModel {
        val builder =
            UpdateModel.Builder(model)
                .setTriesMade(model.triesMade?.plus(1) ?: 1)

        return if (model.retries == null || model.retriesTimeout == null) {
            storageProvider.update(builder.setStatus(Status.FAIL).build())
        } else {
            if ((model.triesMade ?: 0) >= model.retries!!) {
                storageProvider.update(builder.setStatus(Status.FAIL).build())
            } else {
                storageProvider.update(
                    builder
                        .setStatus(Status.WAITING_RETRY)
                        .setNextRetries(LocalDateTime.now().plus(model.retriesTimeout))
                        .build(),
                )
            }
        }
    }

    private suspend fun fatal(
        model: GetModel,
        message: String?,
    ): GetModel {
        return storageProvider.update(
            UpdateModel.Builder(model)
                .setStatus(Status.FATAL)
                .setFatalMessage(message)
                .build(),
        )
    }

    private suspend fun join() {
        joinAll(
            producersSupervisor,
            receiversSupervisor,
            handlersSupervisor,
        )
    }
}
