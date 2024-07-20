package io.github.idmosk.saga.storage.model

import com.fasterxml.uuid.Generators
import io.github.idmosk.saga.storage.model.enums.Status
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID

/**
 * Model for storing in storage by the provider
 * @property id time-based generated UUID v6
 * @property step number of step
 * @property triesMade tries already made
 * @property nextRetries planned time for retry
 * @property fatalMessage for status [FATAL][Status.FATAL]
 * @property businessId id of business process
 * @property stepsForward list of methods
 * @property stepsBackward list of rollback methods
 * @property retries max retries count
 */
data class CreateModel(
    val businessId: String,
    val stepsForward: List<String>,
    val stepsBackward: List<String?>,
    val deadLine: LocalDateTime?,
    val retries: Int?,
    val retriesTimeout: Period?,
) {
    val id: UUID = Generators.timeBasedReorderedGenerator().generate()
    val createdAt: LocalDateTime = LocalDateTime.now()
    val step: Int = 1
    val status: Status = Status.NEW
    val triesMade: Int? = null
    val nextRetries: LocalDateTime? = null
    val fatalMessage: String? = null
    val updatedAt: LocalDateTime? = null
}
