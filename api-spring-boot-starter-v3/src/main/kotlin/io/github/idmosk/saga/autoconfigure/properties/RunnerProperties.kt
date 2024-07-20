package io.github.idmosk.saga.autoconfigure.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "io.github.idmosk.saga.api.runner")
class RunnerProperties(
    /**
     * If all found method in implementations should be enabled
     */
    var allMethodsAreEnabled: Boolean = false,
    /**
     * Concurrency for all found methods with [allMethodsAreEnabled] = true
     */
    var concurrencyForAllMethods: Int = 1,
    /**
     * Comma-separated list of methods for processing + workers count.
     *
     * Might be empty with [allMethodsAreEnabled] = true to process all founded methods with appropriate signature
     *
     * Example: io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward1:1,io.github.idmosk.saga.api.sagas.ThreeStepsOkSaga.backward1:1
     */
    var enabledMethods: Set<String> = setOf(),
)
