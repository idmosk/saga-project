package io.github.idmosk.saga.queue.model

import io.github.idmosk.saga.queue.model.enums.SagasResultEnum
import java.util.UUID

/**
 * Model describing the result of the entire saga
 * @param id sagas ID
 * @param businessId sagas businessId
 * @param sagasResult sagas result
 */
data class SagasResult(
    val id: UUID,
    val businessId: String,
    val sagasResult: SagasResultEnum,
)
