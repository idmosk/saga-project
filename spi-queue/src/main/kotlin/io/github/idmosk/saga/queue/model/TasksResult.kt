package io.github.idmosk.saga.queue.model

import io.github.idmosk.saga.queue.model.enums.TasksResultEnum
import java.util.UUID

/**
 * Model describing the task execution result
 * @property message additional info for [TasksResultEnum.FATAL]
 */
data class TasksResult(
    val id: UUID,
    val result: TasksResultEnum,
    val message: String?,
)
