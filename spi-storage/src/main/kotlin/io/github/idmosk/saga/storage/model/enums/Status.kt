package io.github.idmosk.saga.storage.model.enums

/**
 * Possible statuses of the saga
 */
enum class Status {
    /**
     * Just created
     */
    NEW,

    /**
     * Moving forward
     */
    FORWARD,

    /**
     * Moving backward
     */
    BACKWARD,

    /**
     * Waiting for retry
     */
    WAITING_RETRY,

    /**
     * Expired without starting steps
     */
    EXPIRED,

    /**
     * Completed successfully
     */
    DONE,

    /**
     * Rolled back successfully
     */
    ROLLED_BACK,

    /**
     * Maximum number of forward or backward movement retry reached
     */
    FAIL,

    /**
     * Failure
     */
    FATAL,
}
