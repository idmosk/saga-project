package io.github.idmosk.saga.queue

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.queue.model.SagasResult
import io.github.idmosk.saga.queue.model.Task
import io.github.idmosk.saga.queue.model.TasksResult
import io.github.idmosk.saga.queue.model.enums.Direction
import io.github.idmosk.saga.queue.model.enums.SagasResultEnum
import io.github.idmosk.saga.queue.model.enums.TasksResultEnum
import io.github.idmosk.saga.queue.provider.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * A class that you can inherit from in your tests to test your implementation of [Provider]
 */
abstract class SpiTest {
    @Test
    fun transferNextStep(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<NextStep>()
            val nextStep = NextStep(UUID.randomUUID())
            lateinit var nextStepFromChannel: NextStep

            getProvider().putNextStep(nextStep)

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullNextSteps(this, channel)
                }

            launch(supervisorJob) {
                for (it in channel) {
                    nextStepFromChannel = it
                    channel.close()
                }
            }.join()

            assertEquals(nextStep, nextStepFromChannel)

            producerJob.cancel()
            supervisorJob.complete()
            supervisorJob.join()
        }

    @OptIn(ObsoleteCoroutinesApi::class)
    @Test
    fun multipleNextStepReceivers(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<NextStep>()

            val consumersJobs = mutableSetOf<Job>()
            val count = AtomicInteger()
            val uuids = mutableSetOf<UUID>()

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullNextSteps(this, channel)
                }

            repeat(1000) {
                consumersJobs.add(
                    launch(supervisorJob) {
                        for (nextStep in channel) {
                            assertTrue(uuids.remove(nextStep.id))
                            val c = count.decrementAndGet()
                            assertTrue(c >= 0)
                            if (c == 0) {
                                channel.close()
                            }
                        }
                    },
                )
            }

            repeat(10000) {
                count.incrementAndGet()
                val uuid = UUID.randomUUID()
                uuids.add(uuid)
                getProvider().putNextStep(NextStep(uuid))
            }

            consumersJobs.joinAll()
            producerJob.cancel()

            val mustBeEmptyChannel = Channel<NextStep>()
            val emptyJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullNextSteps(this, mustBeEmptyChannel)
                }
            // getProvider().putNextStep(NextStep(UUID.randomUUID()))

            select {
                mustBeEmptyChannel.onReceive { fail("not empty producer") }
                ticker(initialDelayMillis = 50, delayMillis = 50).onReceive {}
            }

            emptyJob.cancel()

            assertEquals(0, count.get())
            assertEquals(0, uuids.size)

            supervisorJob.complete()
            supervisorJob.join()
        }

    @Test
    fun transferTask(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<Task>()
            val task = Task(UUID.randomUUID(), "businessId", Direction.FORWARD, "method")
            lateinit var taskFromChannel: Task

            getProvider().putTask("methodSecond", task)
            getProvider().putTask("method", task)

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullTasks(this, channel, "method")
                }

            launch(supervisorJob) {
                for (it in channel) {
                    taskFromChannel = it
                    channel.close()
                }
            }.join()

            assertEquals(task, taskFromChannel)

            producerJob.cancel()
            supervisorJob.complete()
            supervisorJob.join()
        }

    @OptIn(ObsoleteCoroutinesApi::class)
    @Test
    fun multipleTaskReceivers(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<Task>()

            val consumersJobs = mutableSetOf<Job>()
            val count = AtomicInteger()
            val uuids = mutableSetOf<UUID>()

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullTasks(this, channel, "method")
                }

            repeat(1000) {
                consumersJobs.add(
                    launch(supervisorJob) {
                        for (task in channel) {
                            assertTrue(uuids.remove(task.id))
                            val c = count.decrementAndGet()
                            assertTrue(c >= 0)
                            if (c == 0) {
                                channel.close()
                            }
                        }
                    },
                )
            }

            getProvider().putTask("methodSecond", Task(UUID.randomUUID(), "businessId", Direction.FORWARD, "methodSecond"))
            repeat(10000) {
                count.incrementAndGet()
                val uuid = UUID.randomUUID()
                uuids.add(uuid)
                getProvider().putTask("method", Task(uuid, "businessId", Direction.FORWARD, "method"))
            }

            consumersJobs.joinAll()
            producerJob.cancel()

            val mustBeEmptyChannel = Channel<Task>()
            val emptyJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullTasks(this, mustBeEmptyChannel, "method")
                }
            getProvider().putTask("methodSecond", Task(UUID.randomUUID(), "businessId", Direction.FORWARD, "methodSecond"))
            // getProvider().putTask("method", Task(UUID.randomUUID(), "businessId", Direction.FORWARD, "method"))

            select {
                mustBeEmptyChannel.onReceive { fail("not empty producer") }
                ticker(initialDelayMillis = 50, delayMillis = 50).onReceive {}
            }

            emptyJob.cancel()

            assertEquals(0, count.get())
            assertEquals(0, uuids.size)

            supervisorJob.complete()
            supervisorJob.join()
        }

    @Test
    fun transferTasksResult(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<TasksResult>()
            val tasksResult = TasksResult(UUID.randomUUID(), TasksResultEnum.FORWARD_OK, null)
            lateinit var tasksResultFromChannel: TasksResult

            getProvider().putTasksResult(tasksResult)

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullTasksResults(this, channel)
                }

            launch(supervisorJob) {
                for (it in channel) {
                    tasksResultFromChannel = it
                    channel.close()
                }
            }.join()

            assertEquals(tasksResult, tasksResultFromChannel)

            producerJob.cancel()
            supervisorJob.complete()
            supervisorJob.join()
        }

    @OptIn(ObsoleteCoroutinesApi::class)
    @Test
    fun multipleTasksResultReceivers(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<TasksResult>()

            val consumersJobs = mutableSetOf<Job>()
            val count = AtomicInteger()
            val uuids = mutableSetOf<UUID>()

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullTasksResults(this, channel)
                }

            repeat(1000) {
                consumersJobs.add(
                    launch(supervisorJob) {
                        for (taskResult in channel) {
                            assertTrue(uuids.remove(taskResult.id))
                            val c = count.decrementAndGet()
                            assertTrue(c >= 0)
                            if (c == 0) {
                                channel.close()
                            }
                        }
                    },
                )
            }

            repeat(10000) {
                count.incrementAndGet()
                val uuid = UUID.randomUUID()
                uuids.add(uuid)
                getProvider().putTasksResult(TasksResult(uuid, TasksResultEnum.FORWARD_OK, null))
            }

            consumersJobs.joinAll()
            producerJob.cancel()

            val mustBeEmptyChannel = Channel<TasksResult>()
            val emptyJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullTasksResults(this, mustBeEmptyChannel)
                }
            // getProvider().putTasksResult(TasksResult(UUID.randomUUID(), TasksResultEnum.FORWARD_OK, null))

            select {
                mustBeEmptyChannel.onReceive { fail("not empty producer") }
                ticker(initialDelayMillis = 50, delayMillis = 50).onReceive {}
            }

            emptyJob.cancel()

            assertEquals(0, count.get())
            assertEquals(0, uuids.size)

            supervisorJob.complete()
            supervisorJob.join()
        }

    @Test
    fun transferSagasResult(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<SagasResult>()
            val sagasResult = SagasResult(UUID.randomUUID(), "businessId", SagasResultEnum.OK)
            lateinit var sagasResultFromChannel: SagasResult

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullSagasResults(this, channel, "businessId")
                }

            getProvider().putSagasResult("businessIdSecond", sagasResult)
            getProvider().putSagasResult("businessId", sagasResult)

            launch(supervisorJob) {
                for (it in channel) {
                    sagasResultFromChannel = it
                    channel.close()
                }
            }.join()

            assertEquals(sagasResult, sagasResultFromChannel)

            producerJob.cancel()
            supervisorJob.complete()
            supervisorJob.join()
        }

    // not determined!
    @Test
    @OptIn(ObsoleteCoroutinesApi::class)
    fun multipleSagasResultReceivers(): Unit =
        runBlocking {
            val supervisorJob = SupervisorJob()
            val channel = Channel<SagasResult>()

            val consumersJobs = mutableSetOf<Job>()
            val count = AtomicInteger()
            val uuids = ConcurrentHashMap.newKeySet<UUID>()

            val producerJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullSagasResults(this, channel, "businessId")
                }

            repeat(1000) {
                consumersJobs.add(
                    launch(supervisorJob) {
                        for (sagasResult in channel) {
                            assertTrue(uuids.remove(sagasResult.id))
                            val c = count.decrementAndGet()
                            assertTrue(c >= 0)
                            if (c == 0) {
                                println("close channel")
                                channel.close()
                            }
                        }
                    },
                )
            }

            getProvider().putSagasResult("businessIdSecond", SagasResult(UUID.randomUUID(), "businessId", SagasResultEnum.OK))
            repeat(10000) {
                count.incrementAndGet()
                val uuid = UUID.randomUUID()
                uuids.add(uuid)
                getProvider().putSagasResult("businessId", SagasResult(uuid, "businessId", SagasResultEnum.OK))
            }

            consumersJobs.joinAll()
            producerJob.cancel()

            delay(100)

            val mustBeEmptyChannel = Channel<SagasResult>()
            val emptyJob =
                with(CoroutineScope(supervisorJob)) {
                    getProvider().pullSagasResults(this, mustBeEmptyChannel, "businessId")
                }
            getProvider().putSagasResult("businessIdSecond", SagasResult(UUID.randomUUID(), "businessId", SagasResultEnum.OK))
            // getProvider().putSagasResult("businessId", SagasResult(UUID.randomUUID(), "businessId", SagasResultEnum.OK))

            select {
                mustBeEmptyChannel.onReceive { fail("not empty producer") }
                ticker(initialDelayMillis = 50, delayMillis = 50).onReceive {}
            }

            emptyJob.cancel()

            assertEquals(0, count.get())
            assertEquals(0, uuids.size)

            supervisorJob.complete()
            supervisorJob.join()
        }

    /**
     * Method that will return an instance of your implementation of [Provider]
     */
    abstract fun getProvider(): Provider
}
