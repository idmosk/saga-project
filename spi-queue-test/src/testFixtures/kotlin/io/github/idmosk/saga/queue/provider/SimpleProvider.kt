package io.github.idmosk.saga.queue.provider

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.queue.model.SagasResult
import io.github.idmosk.saga.queue.model.Task
import io.github.idmosk.saga.queue.model.TasksResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

open class SimpleProvider : Provider {
    protected val nextSteps: BlockingQueue<NextStep>
    protected val tasksResults: BlockingQueue<TasksResult>

    protected val sagasResults: MutableMap<String, BlockingQueue<SagasResult>> = ConcurrentHashMap()
    protected val tasks: MutableMap<String, BlockingQueue<Task>> = ConcurrentHashMap()

    protected val createTaskQueueLock: Lock = ReentrantLock()
    protected val createSagasResultQueueLock: Lock = ReentrantLock()

    @OptIn(DelicateCoroutinesApi::class)
    private val fixedThreadPoolContext = newFixedThreadPoolContext(2, "QUEUE_PROVIDER_CONTEXT")

    init {
        nextSteps = LinkedBlockingQueue()
        tasksResults = LinkedBlockingQueue()
    }

    override fun putNextStep(meta: NextStep) {
        nextSteps.add(meta)
    }

    override fun putTask(
        key: String,
        task: Task,
    ) {
        if (tasks[key] == null) {
            runBlocking {
                createTaskQueueLock.lock()
                try {
                    if (tasks[key] == null) {
                        tasks[key] = LinkedBlockingQueue()
                    }
                } finally {
                    createTaskQueueLock.unlock()
                }
            }
        }
        try {
            tasks[key]!!.add(task)
        } catch (e: Exception) {
            // e.printStackTrace()
            throw e
        }
    }

    override fun putTasksResult(result: TasksResult) {
        tasksResults.add(result)
    }

    override fun putSagasResult(
        key: String,
        sagasResult: SagasResult,
    ) {
        try {
            if (sagasResults[key] != null) {
                sagasResults[key]!!.add(sagasResult)
            }
        } catch (e: Exception) {
            // e.printStackTrace()
            throw e
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullNextSteps(
        scope: CoroutineScope,
        channel: SendChannel<NextStep>,
    ): Job {
        return scope.launch {
            try {
                while (!channel.isClosedForSend && isActive) {
                    channel.send(runInterruptible(Dispatchers.IO) { nextSteps.take() })
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    // e.printStackTrace()
                }
                throw e
            } finally {
                channel.close()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullTasks(
        scope: CoroutineScope,
        channel: SendChannel<Task>,
        key: String,
    ): Job {
        if (tasks[key] == null) {
            createTaskQueueLock.lock()
            try {
                if (tasks[key] == null) {
                    tasks[key] = LinkedBlockingQueue()
                }
            } finally {
                createTaskQueueLock.unlock()
            }
        }
        return scope.launch {
            try {
                while (!channel.isClosedForSend && isActive) {
                    channel.send(runInterruptible(Dispatchers.IO) { tasks[key]!!.take() })
                }
            } catch (e: Exception) {
                // e.printStackTrace()
                throw e
            } finally {
                channel.close()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullTasksResults(
        scope: CoroutineScope,
        channel: SendChannel<TasksResult>,
    ): Job {
        return scope.launch {
            try {
                while (!channel.isClosedForSend && isActive) {
                    channel.send(runInterruptible(Dispatchers.IO) { tasksResults.take() })
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    // e.printStackTrace()
                }
                throw e
            } finally {
                channel.close()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullSagasResults(
        scope: CoroutineScope,
        channel: SendChannel<SagasResult>,
        key: String,
    ): Job {
        if (sagasResults[key] == null) {
            createSagasResultQueueLock.lock()
            try {
                if (sagasResults[key] == null) {
                    sagasResults[key] = LinkedBlockingQueue()
                }
            } finally {
                createSagasResultQueueLock.unlock()
            }
        }
        return scope.launch {
            try {
                while (!channel.isClosedForSend && isActive) {
                    channel.send(runInterruptible(fixedThreadPoolContext) { sagasResults[key]!!.take() })
                }
            } catch (e: Exception) {
                // e.printStackTrace()
                throw e
            } finally {
                sagasResults.remove(key)
                channel.close()
            }
        }
    }
}
