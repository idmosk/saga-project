package io.github.idmosk.saga.storage.provider

import io.github.idmosk.saga.storage.model.CreateModel
import io.github.idmosk.saga.storage.model.GetModel
import io.github.idmosk.saga.storage.model.UpdateModel
import io.github.idmosk.saga.storage.model.enums.Status
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

open class SimpleProvider : Provider {
    private val metas: MutableMap<UUID, Model> = ConcurrentHashMap()
    private val changeStatusLock: Lock = ReentrantLock()

    override suspend fun create(meta: CreateModel): GetModel {
        metas[meta.id] =
            Model(
                meta.id, meta.businessId, meta.createdAt, meta.stepsForward,
                meta.stepsBackward, meta.deadLine, meta.retries, meta.retriesTimeout,
                meta.step, meta.status, meta.triesMade, meta.nextRetries, meta.fatalMessage,
                meta.updatedAt,
            )
        return toGetModel(metas[meta.id]!!)
    }

    override suspend fun update(meta: UpdateModel): GetModel {
        val item = metas[meta.id] ?: run { throw Exception("item not found") }

        meta.updatedFields.forEach {
            when (it) {
                UpdateModel.UpdatableFields.STEP -> item.step = meta.step
                UpdateModel.UpdatableFields.STATUS -> item.status = meta.status
                UpdateModel.UpdatableFields.TRIES_MADE -> item.triesMade = meta.triesMade
                UpdateModel.UpdatableFields.NEXT_RETRIES -> item.nextRetries = meta.nextRetries
                UpdateModel.UpdatableFields.FATAL_MESSAGE -> item.fatalMessage = meta.fatalMessage
                UpdateModel.UpdatableFields.UPDATED_AT -> item.updatedAt = meta.updatedAt
            }
        }

        return toGetModel(item)
    }

    override suspend fun changeStatus(
        id: UUID,
        from: Status,
        to: Status,
    ): Boolean {
        changeStatusLock.lock()
        try {
            val item = metas[id] ?: run { throw Exception("item not found") }

            return if (item.status == from) {
                update(
                    UpdateModel.Builder(get(id))
                        .setStatus(to)
                        .build(),
                )
                true
            } else {
                false
            }
        } finally {
            changeStatusLock.unlock()
        }
    }

    override suspend fun get(uuid: UUID): GetModel {
        return metas[uuid]?.let { toGetModel(it) } ?: run { throw Exception("item not found") }
    }

    override suspend fun fetchForRetry(
        fromCreatedAt: LocalDateTime,
        fromId: UUID?,
        limit: Int,
    ): List<GetModel> {
        val out: MutableList<Model> = mutableListOf()

        metas.values.toSortedSet { o1, o2 ->
            if (o1.createdAt.equals(o2.createdAt)) {
                o1.id.compareTo(o2.id)
            } else {
                o1.createdAt.compareTo(o2.createdAt)
            }
        }.forEach { meta ->
            if (meta.status == Status.WAITING_RETRY && meta.nextRetries?.isBefore(LocalDateTime.now()) == true) {
                if (meta.createdAt > fromCreatedAt ||
                    (
                        meta.createdAt == fromCreatedAt && (
                            fromId == null || meta.id.compareTo(fromId) > 0
                        )
                    )
                ) {
                    out.add(meta)
                }
            }
            if (out.size == limit) {
                return toGetModels(out)
            }
        }

        return toGetModels(out)
    }

    private fun toGetModel(m: Model): GetModel {
        return GetModel(
            m.id, m.businessId, m.createdAt, m.stepsForward, m.stepsBackward,
            m.deadLine, m.retries, m.retriesTimeout, m.step, m.status, m.triesMade,
            m.nextRetries, m.fatalMessage, m.updatedAt,
        )
    }

    private fun toGetModels(ms: List<Model>): List<GetModel> {
        val models: MutableList<GetModel> = mutableListOf()
        ms.forEach { models.add(toGetModel(it)) }
        return models
    }

    // inner model description
    data class Model constructor(
        val id: UUID,
        val businessId: String,
        val createdAt: LocalDateTime,
        val stepsForward: List<String>,
        val stepsBackward: List<String?>,
        val deadLine: LocalDateTime?,
        val retries: Int?,
        val retriesTimeout: Period?,
        var step: Int,
        var status: Status,
        var triesMade: Int?,
        var nextRetries: LocalDateTime?,
        var fatalMessage: String?,
        var updatedAt: LocalDateTime?,
    )
}
