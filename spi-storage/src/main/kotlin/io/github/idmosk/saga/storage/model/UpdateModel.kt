package io.github.idmosk.saga.storage.model

import io.github.idmosk.saga.storage.model.enums.Status
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.Set

/**
 * Model with updated data to be updated in storage by the provider.
 * @param updatedFields affected fields
 */
class UpdateModel private constructor(
    val id: UUID,
    val step: Int,
    val status: Status,
    val triesMade: Int?,
    val nextRetries: LocalDateTime?,
    val fatalMessage: String?,
    val updatedFields: Set<UpdatableFields>,
) {
    val updatedAt: LocalDateTime = LocalDateTime.now()

    private constructor(builder: Builder) : this(
        builder.id,
        builder.step,
        builder.status,
        builder.triesMade,
        builder.nextRetries,
        builder.fatalMessage,
        builder.updatedFields,
    )

    data class Builder(val model: GetModel) {
        val id: UUID = model.id
        var step: Int = model.step
        var status: Status = model.status
        var triesMade: Int? = model.triesMade
        var nextRetries: LocalDateTime? = model.nextRetries
        var fatalMessage: String? = model.fatalMessage

        val updatedFields: MutableSet<UpdatableFields> = mutableSetOf()

        fun setStep(step: Int) =
            apply {
                this.step = step
                updatedFields.add(UpdatableFields.STEP)
            }

        fun setStatus(status: Status) =
            apply {
                this.status = status
                updatedFields.add(UpdatableFields.STATUS)
            }

        fun setTriesMade(triesMade: Int?) =
            apply {
                this.triesMade = triesMade
                updatedFields.add(UpdatableFields.TRIES_MADE)
            }

        fun setNextRetries(nextRetries: LocalDateTime?) =
            apply {
                this.nextRetries = nextRetries
                updatedFields.add(UpdatableFields.NEXT_RETRIES)
            }

        fun setFatalMessage(fatalMessage: String?) =
            apply {
                this.fatalMessage = fatalMessage
                updatedFields.add(UpdatableFields.FATAL_MESSAGE)
            }

        fun build(): UpdateModel {
            updatedFields.add(UpdatableFields.UPDATED_AT)
            return UpdateModel(this)
        }
    }

    enum class UpdatableFields {
        STEP,
        STATUS,
        TRIES_MADE,
        NEXT_RETRIES,
        FATAL_MESSAGE,
        UPDATED_AT,
    }
}
