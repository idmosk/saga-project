package io.github.idmosk.saga.storage

import io.github.idmosk.saga.storage.model.CreateModel
import io.github.idmosk.saga.storage.model.UpdateModel
import io.github.idmosk.saga.storage.model.enums.Status
import io.github.idmosk.saga.storage.provider.Provider
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A class that you can inherit from in your tests to test your implementation of [Provider]
 */
abstract class SpiTest {
    private val createModel =
        CreateModel(
            "BUSS_ID",
            listOf("f1", "f2"),
            listOf("b1", null),
            LocalDateTime.now().plusMinutes(1),
            5,
            Period.ZERO,
        )

    private lateinit var notExistsUpdateModel: UpdateModel

    init {
        UpdateModel::class.java.declaredConstructors.forEach {
            if (it.parameterCount == 7) {
                it.isAccessible = true
                val model = it.newInstance(UUID.randomUUID(), 0, Status.NEW, null, null, null, emptySet<KProperty1<UpdateModel, *>>())

                if (model is UpdateModel) {
                    notExistsUpdateModel = model
                }
            }
        }
    }

    @Test
    fun create(): Unit =
        runBlocking {
            val createdModel = getProvider().create(createModel)

            assertEquals(createdModel.businessId, createModel.businessId)
            assertEquals(createdModel.deadLine, createModel.deadLine)
            assertEquals(createdModel.retries, createModel.retries)
            assertEquals(createdModel.retriesTimeout, createModel.retriesTimeout)
            assertContentEquals(createdModel.stepsForward, createModel.stepsForward)
            assertContentEquals(createdModel.stepsBackward, createModel.stepsBackward)
        }

    @Test
    fun get(): Unit =
        runBlocking {
            val modelCreated = getProvider().create(createModel)
            val model = getProvider().get(modelCreated.id)

            assertEquals(modelCreated, model)
        }

    @Test
    fun emptyUpdate(): Unit =
        runBlocking {
            val createdModel = getProvider().create(createModel)
            var model = getProvider().get(createdModel.id)
            val updatedModel = getProvider().update(UpdateModel.Builder(model).build())

            assertNotNull(updatedModel.updatedAt)

            assertEquals(model.id, updatedModel.id)
            assertEquals(model.businessId, updatedModel.businessId)
            assertEquals(model.createdAt, updatedModel.createdAt)
            assertEquals(model.step, updatedModel.step)
            assertEquals(model.status, updatedModel.status)
            assertEquals(model.deadLine, updatedModel.deadLine)
            assertEquals(model.retries, updatedModel.retries)
            assertEquals(model.nextRetries, updatedModel.nextRetries)
            assertEquals(model.triesMade, updatedModel.triesMade)
            assertEquals(model.retriesTimeout, updatedModel.retriesTimeout)
            assertEquals(model.fatalMessage, updatedModel.fatalMessage)
            assertContentEquals(model.stepsForward, updatedModel.stepsForward)
            assertContentEquals(model.stepsBackward, updatedModel.stepsBackward)

            model = getProvider().get(updatedModel.id)

            assertEquals(updatedModel, model)
        }

    @Test
    fun update(): Unit =
        runBlocking {
            val createdModel = getProvider().create(createModel)

            val nextRetries = LocalDateTime.now().plusHours(1)

            val updatedModel =
                getProvider().update(
                    UpdateModel.Builder(createdModel)
                        .setStatus(Status.DONE)
                        .setStep(1)
                        .setTriesMade(1)
                        .setNextRetries(nextRetries)
                        .setFatalMessage("FATAL")
                        .build(),
                )

            assertNotNull(updatedModel.updatedAt)

            assertEquals(createdModel.id, updatedModel.id)
            assertEquals(createdModel.businessId, updatedModel.businessId)
            assertEquals(createdModel.createdAt, updatedModel.createdAt)
            assertEquals(1, updatedModel.step)
            assertEquals(Status.DONE, updatedModel.status)
            assertEquals(createdModel.deadLine, updatedModel.deadLine)
            assertEquals(createdModel.retries, updatedModel.retries)
            assertEquals(nextRetries, updatedModel.nextRetries)
            assertEquals(1, updatedModel.triesMade)
            assertEquals(createdModel.retriesTimeout, updatedModel.retriesTimeout)
            assertEquals("FATAL", updatedModel.fatalMessage)
            assertContentEquals(createdModel.stepsForward, updatedModel.stepsForward)
            assertContentEquals(createdModel.stepsBackward, updatedModel.stepsBackward)

            val model = getProvider().get(updatedModel.id)

            assertEquals(updatedModel, model)
        }

    @Test
    fun changeStatus(): Unit =
        runBlocking {
            val createdModel = getProvider().create(createModel)

            val ok = getProvider().changeStatus(createdModel.id, createdModel.status, Status.FORWARD)

            assertTrue(ok)

            val model = getProvider().get(createdModel.id)

            assertEquals(Status.FORWARD, model.status)
            assertNotNull(model.updatedAt)

            assertEquals(createdModel.id, model.id)
            assertEquals(createdModel.businessId, model.businessId)
            assertEquals(createdModel.createdAt, model.createdAt)
            assertEquals(createdModel.step, model.step)
            assertEquals(createdModel.deadLine, model.deadLine)
            assertEquals(createdModel.retries, model.retries)
            assertEquals(createdModel.nextRetries, model.nextRetries)
            assertEquals(createdModel.triesMade, model.triesMade)
            assertEquals(createdModel.retriesTimeout, model.retriesTimeout)
            assertContentEquals(createdModel.stepsForward, model.stepsForward)
            assertContentEquals(createdModel.stepsBackward, model.stepsBackward)
        }

    @Test
    fun getForRetry(): Unit =
        runBlocking {
            val uuids: MutableSet<UUID> = mutableSetOf()

            repeat(177) {
                val createdModel =
                    getProvider().create(
                        CreateModel(
                            "BUSS_ID_$it",
                            listOf("f1", "f2"),
                            listOf("b1", null),
                            LocalDateTime.now().plusMinutes(1),
                            5,
                            Period.ZERO,
                        ),
                    )
                uuids.add(createdModel.id)
                getProvider().update(
                    UpdateModel.Builder(createdModel)
                        .setStatus(Status.WAITING_RETRY)
                        .setNextRetries(LocalDateTime.now())
                        .build(),
                )
            }

            var fromCreatedAt = LocalDateTime.MIN
            var fromId: UUID? = null
            while (true) {
                val models = getProvider().fetchForRetry(fromCreatedAt, fromId, 3)
                if (models.isEmpty()) {
                    break
                }
                models.forEach {
                    assertTrue(uuids.remove(it.id))
                }
                fromCreatedAt = models[models.size - 1].createdAt
                fromId = models[models.size - 1].id
            }

            assertEquals(0, uuids.size)
        }

    @Test
    fun changeWrongStatus(): Unit =
        runBlocking {
            val createdModel = getProvider().create(createModel)

            val ok = getProvider().changeStatus(createdModel.id, Status.FORWARD, Status.BACKWARD)

            assertFalse(ok)

            val model = getProvider().get(createdModel.id)

            assertEquals(createdModel.id, model.id)
            assertEquals(createdModel.businessId, model.businessId)
            assertEquals(createdModel.createdAt, model.createdAt)
            assertEquals(createdModel.step, model.step)
            assertEquals(createdModel.deadLine, model.deadLine)
            assertEquals(createdModel.retries, model.retries)
            assertEquals(createdModel.nextRetries, model.nextRetries)
            assertEquals(createdModel.triesMade, model.triesMade)
            assertEquals(createdModel.retriesTimeout, model.retriesTimeout)
            assertContentEquals(createdModel.stepsForward, model.stepsForward)
            assertContentEquals(createdModel.stepsBackward, model.stepsBackward)
            assertEquals(createdModel.status, model.status)
            assertNull(model.updatedAt)
        }

    @Test
    fun getNotExisted() {
        val e = assertFailsWith<Exception> { runBlocking { getProvider().get(UUID.randomUUID()) } }
        assertEquals(e.message, "item not found")
    }

    @Test
    fun updateNotExisted() {
        val e = assertFailsWith<Exception> { runBlocking { getProvider().update(notExistsUpdateModel) } }
        assertEquals(e.message, "item not found")
    }

    /**
     * Method that will return an instance of your implementation of [Provider]
     */
    abstract fun getProvider(): Provider
}
