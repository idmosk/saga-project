package io.github.idmosk.saga.storage.provider

import io.github.idmosk.saga.storage.model.CreateModel
import io.github.idmosk.saga.storage.model.GetModel
import io.github.idmosk.saga.storage.model.UpdateModel
import io.github.idmosk.saga.storage.model.enums.Status
import java.time.LocalDateTime
import java.util.UUID

interface Provider {
    /**
     * Method should store data in storage.
     */
    suspend fun create(meta: CreateModel): GetModel

    /**
     * Method should update existing data in storage by [UpdateModel.id].
     */
    suspend fun update(meta: UpdateModel): GetModel

    /**
     * Method should update sagas [Status] by id.
     *
     * Method should support concurrent update.
     */
    suspend fun changeStatus(
        id: UUID,
        from: Status,
        to: Status,
    ): Boolean

    /**
     * Method should return [GetModel] from storage.
     */
    suspend fun get(uuid: UUID): GetModel

    /**
     * Method should return paginated result with list of [GetModel] from storage
     * @param fromId sortable UUID v6.
     */
    suspend fun fetchForRetry(
        fromCreatedAt: LocalDateTime,
        fromId: UUID?,
        limit: Int,
    ): List<GetModel>
}
