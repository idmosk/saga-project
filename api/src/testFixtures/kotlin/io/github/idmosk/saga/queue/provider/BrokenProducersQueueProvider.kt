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
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.random.Random

class BrokenProducersQueueProvider : SimpleProvider() {
    private val randNextSteps = Random(1)
    private val randTasksResults = Random(2)
    private val randTasks: MutableMap<String, Random> = ConcurrentHashMap()
    private val randSagasResults: MutableMap<String, Random> = ConcurrentHashMap()

    @OptIn(DelicateCoroutinesApi::class)
    private val fixedThreadPoolContext = newFixedThreadPoolContext(2, "BROKEN_QUEUE_PROVIDER_CONTEXT")

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullNextSteps(
        scope: CoroutineScope,
        channel: SendChannel<NextStep>,
    ): Job {
        return scope.launch {
            try {
                while (!channel.isClosedForSend && isActive) {
                    if (randNextSteps.nextInt() % 3 == 0) {
                        throw Exception("Random Exception nextSteps")
                    }
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

    override fun putTask(
        key: String,
        task: Task,
    ) {
        if (randTasks[key] == null) {
            randTasks[key] = Random(key.hashCode())
        }
        super.putTask(key, task)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullTasks(
        scope: CoroutineScope,
        channel: SendChannel<Task>,
        key: String,
    ): Job {
        if (randTasks[key] == null) {
            randTasks[key] = Random(key.hashCode())
        }
        return scope.launch {
            try {
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
                try {
                    while (!channel.isClosedForSend && isActive) {
                        if (randTasks[key]!!.nextInt() % 3 == 0) {
                            throw Exception("Random Exception pullTasks")
                        }
                        channel.send(runInterruptible(Dispatchers.IO) { tasks[key]!!.take() })
                    }
                } catch (_: CancellationException) {
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
                    if (randTasksResults.nextInt() % 3 == 0) {
                        throw Exception("Random Exception tasksResults")
                    }
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

    override fun putSagasResult(
        key: String,
        sagasResult: SagasResult,
    ) {
        if (randSagasResults[key] == null) {
            randSagasResults[key] = Random(key.hashCode())
        }
        super.putSagasResult(key, sagasResult)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun pullSagasResults(
        scope: CoroutineScope,
        channel: SendChannel<SagasResult>,
        key: String,
    ): Job {
        if (randSagasResults[key] == null) {
            randSagasResults[key] = Random(key.hashCode())
        }
        return scope.launch {
            try {
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
                try {
                    while (!channel.isClosedForSend && isActive) {
//                        if (randSagasResults[key]!!.nextInt() % 3 == 0) {//TODO
//                            throw Exception("Random Exception sagasResults")
//                        }
                        channel.send(runInterruptible(fixedThreadPoolContext) { sagasResults[key]!!.take() })
                    }
                } catch (_: CancellationException) {
                } finally {
                    sagasResults.remove(key)
                }
            } catch (e: Exception) {
                // e.printStackTrace()
                throw e
            } finally {
                channel.close()
            }
        }
    }
}
