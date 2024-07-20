package io.github.idmosk.saga.api

import io.github.idmosk.saga.api.creator.Creator
import io.github.idmosk.saga.api.creator.NewSaga
import io.github.idmosk.saga.api.repeater.Repeater
import io.github.idmosk.saga.api.router.Router
import io.github.idmosk.saga.api.runner.Runner
import io.github.idmosk.saga.api.sagas.notsuspendable.TwoStepsOkNotSuspendSaga
import io.github.idmosk.saga.api.sagas.notsuspendable.TwoStepsOkNotSuspendSagaImpl
import io.github.idmosk.saga.api.sagas.notsuspendable.TwoStepsOkRolledBackNotSuspendSaga
import io.github.idmosk.saga.api.sagas.notsuspendable.TwoStepsOkRolledBackNotSuspendSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.ThreeStepsOkRolledBackSaga
import io.github.idmosk.saga.api.sagas.suspendable.ThreeStepsOkRolledBackSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.ThreeStepsOkSaga
import io.github.idmosk.saga.api.sagas.suspendable.ThreeStepsOkSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsNokRolledBackSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsNokRolledBackSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkRolledBackSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkRolledBackSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkRolledBackWithExceptionSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkRolledBackWithExceptionSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithBackwardExceptionSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithBackwardExceptionSagaImpl
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithForwardExceptionSaga
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithForwardExceptionSagaImpl
import io.github.idmosk.saga.api.sagas.withoutbackward.TwoStepsOkOnlyForwardSaga
import io.github.idmosk.saga.api.sagas.withoutbackward.TwoStepsOkOnlyForwardSagaImpl
import io.github.idmosk.saga.queue.provider.BrokenProducersQueueProvider
import io.github.idmosk.saga.queue.provider.BrokenPutsQueueProvider
import io.github.idmosk.saga.storage.model.enums.Status
import io.github.idmosk.saga.storage.provider.BrokenStorageProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.idmosk.saga.queue.provider.SimpleProvider as QueueProvider
import io.github.idmosk.saga.storage.provider.SimpleProvider as StorageProvider

class ApiTest {
    private val routerConcurrency = 3

    private val storageProvider = StorageProvider()
    private val queueProvider = QueueProvider()

    private val creator: Creator =
        Creator(storageProvider, queueProvider)
    private val router: Router = Router(storageProvider, queueProvider, routerConcurrency)
    private val repeater: Repeater = Repeater(storageProvider, queueProvider)

    @Test
    fun enabledMethodsWithoutColonTest() {
        val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkSagaImpl())
        val enabledMethods = setOf<String>().plus("without_colon")
        assertFailsWith<java.lang.IndexOutOfBoundsException> {
            Runner(queueProvider, implementations, enabledMethods)
        }
    }

    @Test
    fun enabledMethodsWithoutNumberTest() {
        val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkSagaImpl())
        val enabledMethods = setOf<String>().plus("ok:not_number")
        assertFailsWith<java.lang.NumberFormatException> {
            Runner(queueProvider, implementations, enabledMethods)
        }
    }

    @Test
    fun doesntExistForwardMethodTest() {
        val e =
            assertFails {
                NewSaga.Builder("businessId", TwoStepsSaga::class)
                    .addStep("not_exist", "backward1")
            }
        assertEquals("class does not contain forward method not_exist", e.message)
    }

    @Test
    fun doesntExistBackwardMethodTest() {
        val e =
            assertFails {
                NewSaga.Builder("businessId", TwoStepsSaga::class)
                    .addStep("forward1", "not_exist")
            }
        assertEquals("class does not contain backward method not_exist", e.message)
    }

    @Test
    fun enabledMethodsMistakenNamingTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkSagaImpl())
            val enabledMethods =
                setOf<String>()
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward1:1")
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.backward1:1")
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.MISTAKE:1")
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.backward2:1")

            val e = assertFails { Runner(queueProvider, implementations, enabledMethods) }

            assertEquals("unknown method io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.MISTAKE", e.message)
        }

    @Test
    fun notEnabledRouterTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFailsWith<TimeoutCancellationException> {
                withTimeout(150) { manager.await() }
            }

            val saga = storageProvider.get(manager.id)
            assertEquals(1, saga.step)
            assertEquals(Status.NEW, saga.status)
            assertNull(saga.updatedAt)

            runner.stop()
        }

    @Test
    fun notEnabledRunnerTest(): Unit =
        runBlocking {
            router.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFailsWith<TimeoutCancellationException> {
                withTimeout(500) { manager.await() }
            }

            val saga = storageProvider.get(manager.id)
            assertEquals(1, saga.step)
            assertEquals(Status.FORWARD, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
        }

    @Test
    fun deadLineNowTest(): Unit =
        runBlocking {
            router.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .deadLine(LocalDateTime.now())
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(1, saga.step)
            assertEquals(Status.EXPIRED, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
        }

    @Test
    fun deadLineSoonTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(ThreeStepsOkSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", ThreeStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .addStep("forward3", "backward3")
                    .retriesTimeout(Period.ZERO)
                    .retries(5)
                    .deadLine(LocalDateTime.now().plusNanos(1000 * 1000000))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true, concurrencyForAllMethods = 2)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(5)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertTrue(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(2, saga.step)
            assertEquals(Status.DONE, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkRolledBackSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkRolledBackSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkRolledBackSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(5)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun threeStepsOkRolledBackWithoutBackwardSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(ThreeStepsOkRolledBackSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", ThreeStepsOkRolledBackSaga::class)
                    .addStep("forward1", null)
                    .addStep("forward2", "backward2")
                    .addStep("forward3", "backward3")
                    .retriesTimeout(Period.ZERO)
                    .retries(5)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkRolledBackWithExceptionSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkRolledBackWithExceptionSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)
            repeater.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkRolledBackWithExceptionSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(3)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
            repeater.stop()
        }

    @Test
    fun twoStepsOkRolledBackWithExceptionWithoutRetrySagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkRolledBackWithExceptionSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkRolledBackWithExceptionSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsNokRolledBackSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsNokRolledBackSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)
            repeater.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsNokRolledBackSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(3)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(-1, saga.step)
            assertEquals(Status.FAIL, saga.status)
            assertEquals(4, saga.triesMade)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
            repeater.stop()
        }

    @Test
    fun twoStepsNokRolledBackWithoutRetrySagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsNokRolledBackSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsNokRolledBackSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(-1, saga.step)
            assertEquals(Status.FAIL, saga.status)
            assertEquals(1, saga.triesMade)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsWithForwardExceptionSagaOkTest(): Unit =
        runBlocking {
            val sagaManagers = mutableSetOf<Triple<UUID, String, Creator.Manager>>()

            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsWithForwardExceptionSagaImpl())
            val enabledMethods =
                setOf<String>()
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithForwardExceptionSaga.forward1:2")
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithForwardExceptionSaga.backward1:2")
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithForwardExceptionSaga.forward2:2")
                    .plus("io.github.idmosk.saga.api.sagas.suspendable.TwoStepsWithForwardExceptionSaga.backward2:2")

            val runner = Runner(queueProvider, implementations, enabledMethods)
            val repeater = Repeater(storageProvider, queueProvider, fetchSize = 2)

            router.start(this)
            runner.start(this)
            repeater.start(this)

            repeat(2) {
                val businessId = "businessId$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsWithForwardExceptionSagaImpl::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .retriesTimeout(Period.ZERO)
                            .retries(1)
                            .deadLine(LocalDateTime.now().plusYears(100))
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            val awaitJobs: MutableSet<Job> = mutableSetOf()

            sagaManagers.forEach {
                awaitJobs.add(
                    launch {
                        assertTrue(
                            it.third
                                .listen(this, it.second)
                                .start()
                                .await(),
                        )
                    },
                )
            }

            awaitJobs.joinAll()

            sagaManagers.forEach {
                val saga = storageProvider.get(it.first)
                assertEquals(2, saga.step)
                assertEquals(Status.DONE, saga.status)
                assertEquals(1, saga.triesMade)
                assertNotNull(saga.updatedAt)
            }

            router.stop()
            runner.stop()
            repeater.stop()
        }

    @Test
    fun twoStepsWithBackwardExceptionSagaRolledBackTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsWithBackwardExceptionSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)
            repeater.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsWithBackwardExceptionSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(1)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertEquals(1, saga.triesMade)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
            repeater.stop()
        }

    @Test
    fun twoStepsWithForwardExceptionAndRolledBackSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsWithForwardExceptionSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsWithForwardExceptionSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(0)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNull(saga.triesMade)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsWithBackwardExceptionSagaNokTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsWithBackwardExceptionSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsWithBackwardExceptionSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .retriesTimeout(Period.ZERO)
                    .retries(0)
                    .deadLine(LocalDateTime.now().plusYears(100))
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(-1, saga.step)
            assertEquals(Status.FAIL, saga.status)
            assertEquals(1, saga.triesMade)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkOnlyForwardSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkOnlyForwardSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkOnlyForwardSaga::class)
                    .addStep("forward1", null)
                    .addStep("forward2", null)
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertTrue(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(2, saga.step)
            assertEquals(Status.DONE, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkNotSuspendSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkNotSuspendSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkNotSuspendSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertTrue(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(2, saga.step)
            assertEquals(Status.DONE, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkRolledBackNotSuspendSagaTest(): Unit =
        runBlocking {
            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkRolledBackNotSuspendSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(this)
            runner.start(this)

            val newSaga =
                NewSaga.Builder("businessId", TwoStepsOkRolledBackNotSuspendSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build()

            val manager =
                creator
                    .create(newSaga)
                    .listen(this, newSaga.businessId)
                    .start()

            assertFalse(manager.await())

            val saga = storageProvider.get(manager.id)
            assertEquals(0, saga.step)
            assertEquals(Status.ROLLED_BACK, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkFourTypesSagasTest(): Unit =
        runBlocking {
            val scope = this
            // val scope = CoroutineScope(newSingleThreadContext("MY_THREAD") + CoroutineExceptionHandler {_, t -> t.printStackTrace() })
            // val scope = CoroutineScope(newFixedThreadPoolContext(8, "MY_THREAD") + CoroutineExceptionHandler { _, t -> t.printStackTrace() })
            // val scope = CoroutineScope(coroutineContext + Dispatchers.Default + CoroutineExceptionHandler {_, t -> t.printStackTrace() })

            val count = 50
            val concurrency = 4
            val sagaManagers = mutableSetOf<Triple<UUID, String, Creator.Manager>>()

            val implementations =
                setOf<io.github.idmosk.saga.api.ISaga>()
                    .plus(TwoStepsOkSagaImpl())
                    .plus(TwoStepsWithForwardExceptionSagaImpl())
                    .plus(TwoStepsOkOnlyForwardSagaImpl())
                    .plus(TwoStepsOkNotSuspendSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true, concurrencyForAllMethods = concurrency)

            router.start(scope)
            runner.start(scope)
            repeater.start(scope)

            repeat(count) {
                val businessId = "TwoStepsOkSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsOkSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            repeat(count) {
                val businessId = "TwoStepsWithForwardExceptionSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsWithForwardExceptionSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .retriesTimeout(Period.ZERO)
                            .retries(1)
                            .deadLine(LocalDateTime.now().plusYears(100))
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            repeat(count) {
                val businessId = "TwoStepsOkOnlyForwardSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsOkOnlyForwardSaga::class)
                            .addStep("forward1", null)
                            .addStep("forward2", null)
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            repeat(count) {
                val businessId = "TwoStepsOkNotSuspendSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder("TwoStepsOkNotSuspendSaga$it", TwoStepsOkNotSuspendSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            val awaitJobs: MutableSet<Job> = mutableSetOf()

            sagaManagers.forEach {
                awaitJobs.add(
                    launch {
                        it.third
                            .listen(scope, it.second)
                            .start()
                            .await()
                    },
                )
            }

            awaitJobs.joinAll()

            sagaManagers.forEach {
                val saga = storageProvider.get(it.first)
                assertEquals(it.second, saga.businessId)
                assertEquals(2, saga.step)
                assertEquals(Status.DONE, saga.status)
                assertNotNull(saga.updatedAt)
            }

            router.stop()
            runner.stop()
            repeater.stop()
        }

    @Test
    fun twoStepsOkFourTypesSagasWithBrokenQueueProviderProducersTest(): Unit =
        runBlocking {
            val scope = this

            val brokenQueueProvider = BrokenProducersQueueProvider()

            val creator =
                Creator(storageProvider, brokenQueueProvider)
            val router = Router(storageProvider, brokenQueueProvider, 1)
            val repeater = Repeater(storageProvider, brokenQueueProvider)

            val count = 15
            val concurrency = 1
            val sagaManagers = mutableSetOf<Triple<UUID, String, Creator.Manager>>()

            val implementations =
                setOf<io.github.idmosk.saga.api.ISaga>()
                    .plus(TwoStepsOkSagaImpl())
                    .plus(TwoStepsWithForwardExceptionSagaImpl())
                    .plus(TwoStepsOkOnlyForwardSagaImpl())
                    .plus(TwoStepsOkNotSuspendSagaImpl())

            val runner = Runner(brokenQueueProvider, implementations, allMethodsAreEnabled = true, concurrencyForAllMethods = concurrency)

            router.start(scope)
            runner.start(scope)
            repeater.start(scope)

            repeat(count) {
                val businessId = "TwoStepsOkSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsOkSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            repeat(count) {
                val businessId = "TwoStepsWithForwardExceptionSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsWithForwardExceptionSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .retriesTimeout(Period.ZERO)
                            .retries(1)
                            .deadLine(LocalDateTime.now().plusYears(100))
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            repeat(count) {
                val businessId = "TwoStepsOkOnlyForwardSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsOkOnlyForwardSaga::class)
                            .addStep("forward1", null)
                            .addStep("forward2", null)
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            repeat(count) {
                val businessId = "TwoStepsOkNotSuspendSaga$it"
                val manager =
                    creator.create(
                        NewSaga.Builder(businessId, TwoStepsOkNotSuspendSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                sagaManagers.add(Triple(manager.id, businessId, manager))
            }

            val awaitJobs: MutableSet<Job> = mutableSetOf()

            sagaManagers.forEach {
                awaitJobs.add(
                    launch {
                        it.third
                            .listen(scope, it.second)
                            .start()
                            .await()
                    },
                )
            }

            awaitJobs.joinAll()

            sagaManagers.forEach {
                val saga = storageProvider.get(it.first)
                assertEquals(it.second, saga.businessId)
                assertEquals(2, saga.step)
                assertEquals(Status.DONE, saga.status)
                assertNotNull(saga.updatedAt)
            }

            router.stop()
            runner.stop()
            repeater.stop()
        }

    @Test
    fun twoStepsOkSagaWithBrokenQueueProviderPutsTest(): Unit =
        runBlocking {
            val scope = this

            val brokenQueueProvider = BrokenPutsQueueProvider()

            val creator =
                Creator(storageProvider, brokenQueueProvider)
            val router = Router(storageProvider, brokenQueueProvider, 1)

            val implementations =
                setOf<io.github.idmosk.saga.api.ISaga>()
                    .plus(TwoStepsOkSagaImpl())

            val runner = Runner(brokenQueueProvider, implementations, allMethodsAreEnabled = true)

            router.start(scope)
            runner.start(scope)

            var manager =
                creator.create(
                    NewSaga.Builder("TwoStepsOkSaga_Broken_putNextStep", TwoStepsOkSaga::class)
                        .addStep("forward1", "backward1")
                        .addStep("forward2", "backward2")
                        .build(),
                )
            var e = assertFailsWith<Exception> { manager.start() }
            assertEquals("Broken putNextStep", e.message)

            brokenQueueProvider.brokenPutNextStep = false

            manager =
                creator.create(
                    NewSaga.Builder("TwoStepsOkSaga_Broken_brokenPutTask", TwoStepsOkSaga::class)
                        .addStep("forward1", "backward1")
                        .addStep("forward2", "backward2")
                        .build(),
                ).start()

            delay(100)
            brokenQueueProvider.brokenPutTask = false

            manager =
                creator.create(
                    NewSaga.Builder("TwoStepsOkSaga_Broken_putTasksResult", TwoStepsOkSaga::class)
                        .addStep("forward1", "backward1")
                        .addStep("forward2", "backward2")
                        .build(),
                ).start()

            delay(100)
            brokenQueueProvider.brokenPutTasksResult = false

            manager =
                creator.create(
                    NewSaga.Builder("TwoStepsOkSaga_Broken_putSagasResult", TwoStepsOkSaga::class)
                        .addStep("forward1", "backward1")
                        .addStep("forward2", "backward2")
                        .build(),
                ).start()

            delay(500)

            var saga = storageProvider.get(manager.id)
            assertEquals("TwoStepsOkSaga_Broken_putSagasResult", saga.businessId)
            assertEquals(2, saga.step)
            assertEquals(Status.DONE, saga.status)
            assertNotNull(saga.updatedAt)

            delay(100)
            brokenQueueProvider.brokenPutSagasResult = false

            val businessId = "TwoStepsOkSaga_Not_Broken"
            manager =
                creator
                    .create(
                        NewSaga.Builder(businessId, TwoStepsOkSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                    .listen(scope, businessId)
                    .start()

            assertTrue(manager.await())

            saga = storageProvider.get(manager.id)
            assertEquals("TwoStepsOkSaga_Not_Broken", saga.businessId)
            assertEquals(2, saga.step)
            assertEquals(Status.DONE, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }

    @Test
    fun twoStepsOkSagaWithBrokenStorageProviderTest(): Unit =
        runBlocking {
            val scope = this

            val brokenStorageProvider = BrokenStorageProvider()

            val creator =
                Creator(brokenStorageProvider, queueProvider)
            val router = Router(brokenStorageProvider, queueProvider, 1)

            val implementations = setOf<io.github.idmosk.saga.api.ISaga>().plus(TwoStepsOkSagaImpl())

            val runner = Runner(queueProvider, implementations, allMethodsAreEnabled = true)

            router.start(scope)
            runner.start(scope)

            var e =
                assertFailsWith<Exception> {
                    creator.create(
                        NewSaga.Builder("TwoStepsOkSaga_Broken_create", TwoStepsOkSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                }
            assertEquals("Broken create", e.message)

            delay(100)
            brokenStorageProvider.brokenCreate = false

            creator.create(
                NewSaga.Builder("TwoStepsOkSaga_Broken_get", TwoStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build(),
            ).start()

            delay(100)
            brokenStorageProvider.brokenGet = false

            creator.create(
                NewSaga.Builder("TwoStepsOkSaga_Broken_update", TwoStepsOkSaga::class)
                    .addStep("forward1", "backward1")
                    .addStep("forward2", "backward2")
                    .build(),
            ).start()

            delay(100)
            brokenStorageProvider.brokenUpdate = false

            val businessId = "TwoStepsOkSaga_Not_Broken"
            val manager =
                creator
                    .create(
                        NewSaga.Builder(businessId, TwoStepsOkSaga::class)
                            .addStep("forward1", "backward1")
                            .addStep("forward2", "backward2")
                            .build(),
                    )
                    .start()
                    .listen(scope, businessId)

            assertTrue(manager.await())

            val saga = brokenStorageProvider.get(manager.id)
            assertEquals("TwoStepsOkSaga_Not_Broken", saga.businessId)
            assertEquals(2, saga.step)
            assertEquals(Status.DONE, saga.status)
            assertNotNull(saga.updatedAt)

            router.stop()
            runner.stop()
        }
}
