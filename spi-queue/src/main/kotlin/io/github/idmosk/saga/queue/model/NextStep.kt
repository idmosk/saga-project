package io.github.idmosk.saga.queue.model

import java.util.UUID

/**
 * Model describing the task to be routed
 * @param id sagas ID
 */
data class NextStep(val id: UUID)
