package io.github.idmosk.saga.queue.provider

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.queue.model.SagasResult
import io.github.idmosk.saga.queue.model.Task
import io.github.idmosk.saga.queue.model.TasksResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel

interface Provider {
    /**
     * Method should put [NextStep] to queue
     */
    fun putNextStep(meta: NextStep)

    /**
     * Method should infinitely pull [NextStep] from queue to channel.
     *
     * Method shouldn't catch any exceptions.
     *
     * Method should close channel in finally block.
     * @param scope the scope on which the coroutine should be launched
     */
    fun pullNextSteps(
        scope: CoroutineScope,
        channel: SendChannel<NextStep>,
    ): Job

    /**
     * Method should put [Task] to queue
     * @param key the name of queue
     */
    fun putTask(
        key: String,
        task: Task,
    )

    /**
     * Method should infinitely pull [Task] from queue to channel.
     *
     * Method shouldn't catch any exceptions.
     *
     * Method should close channel in finally block.
     *
     * @param scope the scope on which the coroutine should be launched
     * @param key the name of queue
     */
    fun pullTasks(
        scope: CoroutineScope,
        channel: SendChannel<Task>,
        key: String,
    ): Job

    /**
     * Method should put [TasksResult] to queue
     */
    fun putTasksResult(result: TasksResult)

    /**
     * Method should infinitely pull [TasksResult] from queue to channel.
     *
     * Method shouldn't catch any exceptions.
     *
     * Method should close channel in finally block.
     * @param scope the scope on which the coroutine should be launched
     */
    fun pullTasksResults(
        scope: CoroutineScope,
        channel: SendChannel<TasksResult>,
    ): Job

    /**
     * Method should put [SagasResult] to queue
     * @param key the name of queue
     */
    fun putSagasResult(
        key: String,
        sagasResult: SagasResult,
    )

    /**
     * Method should infinitely pull [SagasResult] from queue to channel.
     *
     * Method shouldn't catch any exceptions.
     *
     * Method should close channel in finally block.
     *
     * @param scope the scope on which the coroutine should be launched
     * @param key the name of queue
     */
    fun pullSagasResults(
        scope: CoroutineScope,
        channel: SendChannel<SagasResult>,
        key: String,
    ): Job
}
