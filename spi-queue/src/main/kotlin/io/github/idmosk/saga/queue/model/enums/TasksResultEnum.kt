package io.github.idmosk.saga.queue.model.enums

/**
 * Possible tasks result
 */
enum class TasksResultEnum {
    /**
     * Step completed successfully. You can move on to the next one in forward direction
     */
    FORWARD_OK,

    /**
     * step completed successfully. You can move on to the next one in forward direction
     */
    FORWARD_NOK,

    /**
     * Step completed successfully. You can move on to the next one in backward direction
     */
    BACKWARD_OK,

    /**
     * Step completed with exception. You should retry or rolled back
     */
    FORWARD_EXCEPTION,

    /**
     * Step completed with exception. You should retry or mark as fatal
     */
    BACKWARD_EXCEPTION,

    /**
     * Unexpected situation
     */
    FATAL,
}
