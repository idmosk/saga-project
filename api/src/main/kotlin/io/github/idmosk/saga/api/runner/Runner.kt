package io.github.idmosk.saga.api.runner

import io.github.idmosk.saga.api.ISaga
import io.github.idmosk.saga.queue.model.Task
import io.github.idmosk.saga.queue.model.TasksResult
import io.github.idmosk.saga.queue.model.enums.Direction
import io.github.idmosk.saga.queue.model.enums.TasksResultEnum
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction1
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider

/**
 * A class for running sagas steps
 * @property implementations how many coroutines will be started
 * @property enabledMethods Comma-separated list of methods for processing + workers count.
 *
 * Might be empty with [allMethodsAreEnabled] = true to process all founded methods with appropriate signature
 *
 * Example: io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward1:1,io.github.idmosk.saga.api.sagas.ThreeStepsOkSaga.backward1:1
 * @property allMethodsAreEnabled if all found method in implementations should be enabled
 * @property concurrencyForAllMethods concurrency for all found methods with [allMethodsAreEnabled] = true
 */
class Runner(
    private val queueProvider: QueueProvider,
    private val implementations: Set<io.github.idmosk.saga.api.ISaga>,
    private val enabledMethods: Set<String> = setOf(),
    private val allMethodsAreEnabled: Boolean = false,
    private val concurrencyForAllMethods: Int = 1,
) {
    private val forwardMethods: MutableMap<String, Pair<io.github.idmosk.saga.api.ISaga, KFunction1<String, Boolean>>> = mutableMapOf()
    private val backwardMethods: MutableMap<String, Pair<io.github.idmosk.saga.api.ISaga, KFunction1<String, Unit>>> = mutableMapOf()
    private val enabledMethodsMap: MutableMap<String, Int> = mutableMapOf()

    init {
        if (!allMethodsAreEnabled) {
            enabledMethods.forEach {
                val classAndMethod = it.split(":")[0]
                val concurrency = it.split(":")[1]

                enabledMethodsMap[classAndMethod] = concurrency.toInt()
            }
        }
        implementations.forEach { iSaga ->
            val clazz = iSaga::class
            var name = clazz.qualifiedName
            if (name?.contains("Impl") == true) {
                name = name.substring(0, name.lastIndexOf("Impl"))
            }
            clazz.members
                .filterIsInstance<KFunction1<String, *>>()
                .filter {
                    it.parameters.size == 2 ||
                        it.parameters.get(0).kind == KParameter.Kind.INSTANCE ||
                        it.parameters.get(1).type.classifier == String::class
                }
                .filter { it.returnType.classifier == Boolean::class || it.returnType.classifier == Unit::class }
                .filter { it.name != "equals" }
                .forEach { m ->
                    if (m.returnType.classifier == Boolean::class) {
                        @Suppress("UNCHECKED_CAST")
                        forwardMethods.put(name + "." + m.name, Pair(iSaga, m as KFunction1<String, Boolean>))
                        if (allMethodsAreEnabled) {
                            enabledMethodsMap[name + "." + m.name] = concurrencyForAllMethods
                        }
                    } else if (m.returnType.classifier == Unit::class) {
                        @Suppress("UNCHECKED_CAST")
                        backwardMethods.put(name + "." + m.name, Pair(iSaga, m as KFunction1<String, Unit>))
                        if (allMethodsAreEnabled) {
                            enabledMethodsMap[name + "." + m.name] = concurrencyForAllMethods
                        }
                    }
                }
        }

        enabledMethodsMap.forEach {
            if (forwardMethods[it.key] == null && backwardMethods[it.key] == null) {
                throw Exception("unknown method ${it.key}")
            }
        }
    }

    private lateinit var parentScope: CoroutineScope

    private lateinit var producersSupervisor: CompletableJob
    private val receiversSupervisor: CompletableJob = SupervisorJob()
    private val handlersSupervisor: CompletableJob = SupervisorJob()

    private val tasksChannels: MutableMap<String, Channel<Task>> = ConcurrentHashMap()
    private val tasksProducerJobs: MutableSet<Job> = Collections.synchronizedSet(mutableSetOf())

    private val tasksUndeliveredElementHandler = fun (e: Task) {
        queueProvider.putTask(e.method, e)
    }

    private val tasksProducerExceptionHandler =
        CoroutineExceptionHandler { context, e ->
            e.printStackTrace()
            run {
                val methodElement = context[MethodContextElement]
                val concurrencyElement = context[ConcurrencyContextElement]
                if (methodElement != null && concurrencyElement != null) {
                    tasksProducerExceptionHandler(methodElement.method, concurrencyElement.concurrency)
                }
            }
        }

    private val tasksReceiverExceptionHandler =
        CoroutineExceptionHandler { context, e ->
            e.printStackTrace()
            run {
                context[MethodContextElement]?.let {
                    tasksReceiverExceptionHandler(it.method)
                }
            }
        }

    private val tasksHandleExceptionHandler =
        CoroutineExceptionHandler { _, t ->
            t.printStackTrace(); // TODO
        }

    private fun tasksProducerExceptionHandler(
        method: String,
        concurrency: Int,
    ) {
        parentScope.launch { launchTasks(method, concurrency) }
    }

    private fun tasksReceiverExceptionHandler(method: String) {
        parentScope.launch { launchTasksReceiver(method) }
    }

    class MethodContextElement(val method: String) : CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<MethodContextElement>

        override val key: CoroutineContext.Key<MethodContextElement>
            get() = Key
    }

    class ConcurrencyContextElement(val concurrency: Int) : CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<ConcurrencyContextElement>

        override val key: CoroutineContext.Key<ConcurrencyContextElement>
            get() = Key
    }

    /**
     * Method that starts a prepared [Runner]
     */
    fun start(scope: CoroutineScope) {
        parentScope = scope
        producersSupervisor = SupervisorJob(parentScope.coroutineContext.job)

        parentScope.launch {
            (forwardMethods.keys + backwardMethods.keys).forEach { method ->
                val concurrency = enabledMethodsMap[method]
                if (concurrency != null) {
                    launchTasks(method, concurrency)
                }
            }

            println("runner started")
            join()
            println("runner ended")
        }
    }

    /**
     * The method that should be called to shut down [Runner] gracefully
     */
    suspend fun stop() {
        tasksChannels.values.forEach { it.close() }

        tasksProducerJobs.forEach { it.cancel() }

        producersSupervisor.complete()
        receiversSupervisor.complete()
        handlersSupervisor.complete()

        join()
    }

    private suspend fun join() {
        joinAll(
            producersSupervisor,
            receiversSupervisor,
            handlersSupervisor,
        )
    }

    private fun CoroutineScope.launchTasks(
        method: String,
        concurrency: Int,
    ) = launch {
        tasksChannels[method] = Channel(onUndeliveredElement = tasksUndeliveredElementHandler)

        launchTasksProducer(method, concurrency)
        repeat(concurrency) { launchTasksReceiver(method) }
    }

    private fun CoroutineScope.launchTasksProducer(
        method: String,
        concurrency: Int,
    ) = launch(
        coroutineContext + producersSupervisor +
            MethodContextElement(method) + ConcurrencyContextElement(concurrency) +
            tasksProducerExceptionHandler,
    ) {
        tasksProducerJobs.add(queueProvider.pullTasks(this, tasksChannels[method]!!, method))
    }

    private fun CoroutineScope.launchTasksReceiver(method: String) =
        launch(
            coroutineContext + receiversSupervisor +
                MethodContextElement(method) + tasksReceiverExceptionHandler,
        ) {
            for (it in tasksChannels[method]!!) {
                handleTask(it, method)
            }
        }

    private suspend fun handleTask(
        task: Task,
        method: String,
    ) = coroutineScope {
        withContext(coroutineContext + handlersSupervisor + tasksHandleExceptionHandler) {
            queueProvider.putTasksResult(
                runMethod(task, method),
            )
        }
    }

    private suspend fun runMethod(
        task: Task,
        method: String,
    ): TasksResult {
        return when (task.direction) {
            Direction.FORWARD -> runForwardMethod(task, method)
            Direction.BACKWARD -> runBackwardMethod(task, method)
        }
    }

    private suspend fun runForwardMethod(
        task: Task,
        method: String,
    ): TasksResult {
        forwardMethods[method]?.let {
            try {
                if (it.second.callSuspend(it.first, task.businessId)) {
                    return TasksResult(task.id, TasksResultEnum.FORWARD_OK, null)
                } else {
                    return TasksResult(task.id, TasksResultEnum.FORWARD_NOK, null)
                }
            } catch (ex: Exception) {
                return TasksResult(task.id, TasksResultEnum.FORWARD_EXCEPTION, ex.message)
            }
        } ?: run {
            return TasksResult(task.id, TasksResultEnum.FATAL, "couldn't find appropriate forward method implementation")
        }
    }

    private suspend fun runBackwardMethod(
        task: Task,
        method: String,
    ): TasksResult {
        backwardMethods[method]?.let {
            try {
                it.second.callSuspend(it.first, task.businessId)
            } catch (ex: Exception) {
                return TasksResult(task.id, TasksResultEnum.BACKWARD_EXCEPTION, ex.message)
            }
            return TasksResult(task.id, TasksResultEnum.BACKWARD_OK, null)
        } ?: run {
            return TasksResult(task.id, TasksResultEnum.FATAL, "couldn't find appropriate backward method implementation")
        }
    }
}
