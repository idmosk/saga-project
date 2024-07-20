package io.github.idmosk.saga.storage.model

import io.github.idmosk.saga.storage.model.enums.Status
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID

/**
 * Model that the provider must return
 */
data class GetModel constructor(
    val id: UUID,
    val businessId: String,
    val createdAt: LocalDateTime,
    val stepsForward: List<String>,
    val stepsBackward: List<String?>,
    val deadLine: LocalDateTime?,
    val retries: Int?,
    val retriesTimeout: Period?,
    val step: Int,
    val status: Status,
    val triesMade: Int?,
    val nextRetries: LocalDateTime?,
    val fatalMessage: String?,
    val updatedAt: LocalDateTime?,
)
