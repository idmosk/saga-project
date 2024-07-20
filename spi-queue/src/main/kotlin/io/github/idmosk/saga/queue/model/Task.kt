package io.github.idmosk.saga.queue.model

import io.github.idmosk.saga.queue.model.enums.Direction
import java.util.UUID

/**
 * Model describing the task to be launched
 * @param id sagas ID
 * @param businessId sagas businessId
 * @param direction direction of movement that will be taken into account when running the method
 * @param method business methods name to launch
 */
data class Task(
    val id: UUID,
    val businessId: String,
    val direction: Direction,
    val method: String,
)
